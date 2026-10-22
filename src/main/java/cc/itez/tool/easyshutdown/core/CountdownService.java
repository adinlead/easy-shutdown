package cc.itez.tool.easyshutdown.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * 倒计时服务：与 UI 无关的状态机。
 * 回调在调度线程上触发，UI 层需自行切回事件线程（Swing 用 SwingUtilities.invokeLater）。
 */
public final class CountdownService implements AutoCloseable {

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private int remaining;
    private IntConsumer onTick;
    private Runnable onFinish;

    public CountdownService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "countdown-timer");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 开始倒计时。重复调用会先取消上一次倒计时。
     *
     * @param seconds  总秒数
     * @param onTick   每秒回调，参数为剩余秒数
     * @param onFinish 倒计时结束回调
     */
    public synchronized void start(int seconds, IntConsumer onTick, Runnable onFinish) {
        cancel();
        this.remaining = seconds;
        this.onTick = onTick;
        this.onFinish = onFinish;
        this.future = executor.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void cancel() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private void tick() {
        IntConsumer tickCallback;
        Runnable finishCallback;
        boolean finished;
        synchronized (this) {
            remaining--;
            finished = remaining <= 0;
            if (finished) {
                cancel();
            }
            tickCallback = onTick;
            finishCallback = onFinish;
        }
        if (tickCallback != null) {
            tickCallback.accept(finished ? 0 : remaining); // 结束时先把 00:00:00 推给 UI
        }
        if (finished && finishCallback != null) {
            finishCallback.run();
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
