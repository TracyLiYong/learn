package com.billow.job.util;

import com.billow.job.constant.JobCst;
import com.billow.job.core.enumType.AutoTaskJobStatusEnum;
import com.billow.job.pojo.ex.MailEx;
import com.billow.job.pojo.vo.ScheduleJobLogVo;
import com.billow.job.pojo.vo.ScheduleJobVo;
import com.billow.job.service.JobService;
import com.billow.job.service.ScheduleJobLogService;
import com.billow.job.service.ScheduleJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 自动任务工具类
 *
 * @author liuyongtao
 * @date 2017年5月8日 上午10:24:57
 */
@Slf4j
public class TaskUtils {

    /**
     * 通过反射调用scheduleJob中定义的方法
     *
     * @param scheduleJob
     */
    public static void invokMethod(ScheduleJobVo scheduleJob) throws Exception {
        Object object = null;
        Class<?> clazz;
        String classType = scheduleJob.getClassType();
        String runClass = scheduleJob.getRunClass();
        if (JobCst.CLASS_TYPE_SPRING_ID.equals(classType)) {
            object = JobContextUtil.getBean(runClass);
        } else if (JobCst.CLASS_TYPE_BEAN_CLASS.equals(classType)) {
            clazz = Class.forName(runClass);
            object = clazz.newInstance();
        }
        if (object == null) {
            throw new RuntimeException("任务名称 = [" + scheduleJob.getJobName() + "] 未启动成功，请检查是否配置正确！！！");
        }
        clazz = object.getClass();
        // 获取自动任务要执行的方法
        Method method = clazz.getDeclaredMethod(scheduleJob.getMethodName());
        if (method == null) {
            throw new RuntimeException("任务名称 = [" + scheduleJob.getJobName() + "] 方法名设置错误！！！");
        }
        method.invoke(object);
    }

    /**
     * cron 表达式计划运行时间
     *
     * @param cron  表达式
     * @param times 运行次数
     * @return java.util.List<java.lang.String>
     * @author LiuYongTao
     * @date 2019/8/13 9:31
     */
    public static List<String> runTime(String cron, int times) {
        List<String> rs = new ArrayList<>();
        if (cron == null || "".equals(cron.trim())) {
            return rs;
        }
        try {
            CronExpression ce = new CronExpression(cron);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = new Date();
            for (int i = 0; i < times; i++) {
                d = ce.getNextValidTimeAfter(d);
                if (d != null) {
                    rs.add(format.format(d));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return rs;
    }

    /**
     * 保存日志，修改任务状态，发送邮件
     *
     * @param scheduleJob
     * @param exception
     * @return void
     * @author LiuYongTao
     * @date 2019/8/22 8:25
     */
    public static void saveLog(ScheduleJobVo scheduleJob, Exception exception) {

        boolean isSuccess = true;
        if (exception != null) {
            isSuccess = false;
        }

        // 插入日志
        ScheduleJobLogVo logDto = null;
        if (scheduleJob.getIsSaveLog()) {
            logDto = new ScheduleJobLogVo();
            logDto.setJobId(scheduleJob.getId());
            logDto.setJobGroup(scheduleJob.getJobGroup());
            logDto.setJobName(scheduleJob.getJobName());
            logDto.setIsSuccess(isSuccess);
            logDto.setRunTime(scheduleJob.getRunTime());
            logDto.setCreatorCode("JOB-AUTO");
            logDto.setCreateTime(new Date());
            logDto.setUpdaterCode("JOB-AUTO");
            logDto.setUpdateTime(new Date());
            if (exception != null) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw, true));
                logDto.setInfo(sw.toString());
                log.error(logDto.getInfo());
            }

            try {
                ScheduleJobLogService scheduleJobLogService = JobContextUtil.getBean(JobCst.SCHEDULE_JOB_LOG_SERVICE_IMPL);
                scheduleJobLogService.insert(logDto);
            } catch (Exception e) {
                log.error("自动任务日志插入失败：{}", e.getMessage());
            }
        }

        // 异常时，是否停止自动任务
        if (!isSuccess && scheduleJob.getIsExceptionStop()) {
            try {
                ScheduleJobService scheduleJobService = JobContextUtil.getBean(JobCst.SCHEDULE_JOB_SERVICE_IMPL);
                ScheduleJobVo scheduleJobVo = scheduleJobService.findByIdAndValidIndIsTrueAndIsStopIsTrue(scheduleJob.getId());
                if (scheduleJobVo != null) {
                    scheduleJobVo.setJobStatus(AutoTaskJobStatusEnum.JOB_STATUS_EXCEPTION.getStatus());
                    scheduleJobService.updateByPk(scheduleJobVo);
                }
            } catch (Exception e) {
                log.error("自动任务修改失败：{}", e.getMessage());
            }
        }

        if (!JobCst.JOB_FC_SEND_MAIL_NO_SEND.equals(scheduleJob.getIsSendMail())) {
            if (ToolsUtils.isEmpty(scheduleJob.getMailReceive())) {
                log.error("邮件发送失败，接收邮件人为空");
                return;
            }

            try {
                MailEx mailEx = new MailEx();
                mailEx.setJobId(scheduleJob.getId());
                mailEx.setJobName(scheduleJob.getJobName());
                mailEx.setToEmails(scheduleJob.getMailReceive());
                mailEx.setSubject(scheduleJob.getJobName() + " 自动任务执行情况");
                mailEx.setMailTemplateId(scheduleJob.getTemplateId());
                if (logDto != null) {
                    mailEx.setLogId(logDto.getId());
                }
                // 发送邮件接口
                JobService jobService = JobContextUtil.getBean(JobCst.JOB_SERVICE_IMPL);
                jobService.sendMail(mailEx);
            } catch (Exception e) {
                log.error("发送邮件发送消息失败：{}", e.getMessage());
            }
        }
    }
}