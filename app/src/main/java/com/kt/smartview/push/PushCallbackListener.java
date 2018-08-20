package com.kt.smartview.push;


import com.kt.smartview.push.deque.LIFOLinkedBlockingDeque;
import com.kt.smartview.push.deque.QueueProcessingType;
import com.kt.smartview.push.service.MqttService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public interface PushCallbackListener {
    public void messageArrived(byte[] payload);
    public MqttService getMqttService();

    class PushThreadFactory {
        public static final int DEFAULT_THREAD_POOL_SIZE = 3;
        public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2;
        public static final QueueProcessingType DEFAULT_TASK_PROCESSING_TYPE = QueueProcessingType.FIFO;

        /** Creates default implementation of task executor */
        public static Executor createExecutor(int threadPoolSize, int threadPriority,
                                              QueueProcessingType tasksProcessingType) {
            boolean lifo = tasksProcessingType == QueueProcessingType.LIFO;
            BlockingQueue<Runnable> taskQueue =
                    lifo ? new LIFOLinkedBlockingDeque<Runnable>() : new LinkedBlockingQueue<Runnable>();
            return new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, taskQueue,
                    createThreadFactory(threadPriority, "uil-pool-"));
        }

        /** Creates default implementation of task distributor */
        public static Executor createTaskDistributor() {
            return Executors.newCachedThreadPool(createThreadFactory(Thread.NORM_PRIORITY, "uil-pool-d-"));
        }

        /** Creates default implementation of {@linkplain ThreadFactory thread factory} for task executor */
        private static ThreadFactory createThreadFactory(int threadPriority, String threadNamePrefix) {
            return new DefaultThreadFactory(threadPriority, threadNamePrefix);
        }

        private static class DefaultThreadFactory implements ThreadFactory {

            private static final AtomicInteger poolNumber = new AtomicInteger(1);

            private final ThreadGroup group;
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix;
            private final int threadPriority;

            DefaultThreadFactory(int threadPriority, String threadNamePrefix) {
                this.threadPriority = threadPriority;
                group = Thread.currentThread().getThreadGroup();
                namePrefix = threadNamePrefix + poolNumber.getAndIncrement() + "-thread-";
            }

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
                if (t.isDaemon()) t.setDaemon(false);
                t.setPriority(threadPriority);
                return t;
            }
        }
    }
}
