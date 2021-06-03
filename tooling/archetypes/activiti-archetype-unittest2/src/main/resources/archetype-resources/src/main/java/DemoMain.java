package com.klc.activiti.demo.helloworld;


import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author PC-KLC
 */
// @Slf4j
public class DemoMain {

    private static final Logger log = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {
        log.info("启动程序");

        // 1.创建流程引擎

        ProcessEngine processEngine = getProcessEngine();

        // 2.部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 3.启动运行流程

        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);
        log.info("启动流程 {}", processInstance.getProcessDefinitionKey());

        // 4.处理流程任务
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            for (Task task : taskList) {
                log.info("待处理任务 {}", task.getName());
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                Map<String, Object> variables = getMap(scanner, formProperties);
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.
                        getRuntimeService().
                        createProcessInstanceQuery().
                        processInstanceId(processInstance.getId()).singleResult();
            }
            log.info("待处理任务数量 {}", taskList.size());
        }


        log.info("关闭程序");
    }

    private static Map<String, Object> getMap(Scanner scanner, List<FormProperty> formProperties) throws ParseException {
        Map<String, Object> variables = Maps.newHashMap();
        for (FormProperty formProperty : formProperties) {
            String line = null;

            if (StringFormType.class.isInstance(formProperty.getType())) {
                log.info("请输入内容 {} ?", formProperty.getName());
                line = scanner.nextLine();
                variables.put(formProperty.getId(), line);
            } else if (DateFormType.class.isInstance(formProperty.getType())) {
                log.info("请输入内容 {} ?  格式是(yyyy-MM-dd)", formProperty.getName());
                line = scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(formProperty.getId(), date);
            } else {
                log.info("类型暂不支持 {}", formProperty.getType());
            }
            log.info("您输入的内容是 [{}]", line);
        }
        return variables;
    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();
        String deploymentId = deploy.getId();
        ProcessDefinition processDefinition = repositoryService.
                                    createProcessDefinitionQuery().
                                    deploymentId(deploymentId).
                                    singleResult();
        log.info("流程定义文件{}, 流程ID{}", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration standaloneInMemProcessEngineConfiguration =
                ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = standaloneInMemProcessEngineConfiguration.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        log.info("流程引擎名称{}, 版本{}", name, version);
        return processEngine;
    }


}
