package cc.itez.tool.easyshutdown.core;

import cc.itez.tool.easyshutdown.Logger;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * MP3 播放服务（jlayer 封装）。同一时间只播放一个音频，重复播放会先停止上一个。
 * 播放线程为守护线程，不会阻止 JVM 退出。
 */
public final class AudioService {

    private Player player;

    public synchronized void play(InputStream source) {
        stop();
        try {
            player = new Player(new BufferedInputStream(source));
        } catch (JavaLayerException e) {
            throw new OperationException("初始化音频播放器失败", e);
        }
        Thread thread = new Thread(() -> {
            try {
                player.play();
            } catch (JavaLayerException e) {
                Logger.error(e, "音频播放中断");
            }
        }, "audio-player");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        if (player != null) {
            player.close();
            player = null;
        }
    }
}
