package com.fgsqw.lanshare.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParameterizedTaskQueue<T> {
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final TaskProcessor<T> taskProcessor;
    private volatile boolean running = true;

    // 自定义回调接口，替代 Consumer
    public interface TaskProcessor<T> {
        void process(T taskParameter);
    }

    public ParameterizedTaskQueue(TaskProcessor<T> taskProcessor) {
        this.taskProcessor = taskProcessor;
    }

    // 添加带参数的任务
    public void addTask(T taskParameter) {
        try {
            queue.put(taskParameter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 消费任务（带参数）
    public void consumeTasks() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞等待任务参数
                T taskParameter = queue.take();
                // 处理带参数的任务
                taskProcessor.process(taskParameter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        running = false;
    }
}

