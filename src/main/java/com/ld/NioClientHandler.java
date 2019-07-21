package com.ld;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioClientHandler implements Runnable {
    private Selector selector;

    public NioClientHandler(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                /**
                 * TODO 获取可用的channel的数量
                 */
                int readyChannels = selector.select();
                /**
                 * TODO 为什么要这样？
                 */
                if (readyChannels == 0) {
                    continue;
                }
                /**
                 * 获取可用channel的集合
                 */
                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    /**
                     * selectionKey实例
                     */
                    SelectionKey selectionKey = iterator.next();
                    /**
                     * 移除Set中的当前selectionKey
                     */
                    iterator.remove();

                    /**
                     * 根据就绪状态，调用对应方法处理业务逻辑
                     */

                    /**
                     * 如果是 可读事件
                     */
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        readHandler(selectionKey, selector);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        /**
         * 要从 selectionKey 中获取已经就绪的channel
         */
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        /**
         * 创建buffer
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        /**
         * 循环读取客户端请求信息
         */
        String response = "";
        while (socketChannel.read(byteBuffer) > 1) {
            /**
             * 切换buffer为读模式
             */
            byteBuffer.flip();

            /**
             * 读取buffer中的内容
             */
            response += Charset.forName("UTF-8").decode(byteBuffer);
        }

        /**
         * 将channel再次注册到selector上，监听他的可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 将服务响应信息 打印到本地
         */
        if (response.length() > 0) {
            //广播给其他客户端
            System.out.println(response);
        }
    }
}
