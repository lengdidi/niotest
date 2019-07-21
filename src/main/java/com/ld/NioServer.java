package com.ld;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO服务端
 */
public class NioServer {
    /**
     * 启动方法
     */
    public void start() throws IOException {
        /**
         * 创建Selector
         */
        Selector selector = Selector.open();

        /**
         * 用过ServerSocketChannel创建channel通道
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        /**
         * 为channel通道绑定监听端口
         */
        serverSocketChannel.bind(new InetSocketAddress(8888));

        /**
         * 设置channel为非阻塞模式
         */
        serverSocketChannel.configureBlocking(false);

        /**
         * 讲channel注册到selector上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功");

        /**
         * 循环等待新接入的连接
         */
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
                 * 如果是 接入事件
                 */
                if (selectionKey.isValid() && selectionKey.isAcceptable()) {
                    acceptHandler(serverSocketChannel, selector);
                }

                /**
                 * 如果是 可读事件
                 */
                if (selectionKey.isValid() && selectionKey.isReadable()) {
                    readHandler(selectionKey, selector);
                }
            }
        }
    }

    /**
     * 接入事件处理器
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        /**
         * 如果要是接入事件，创建socketChannel
         */
        SocketChannel socketChannel = serverSocketChannel.accept();

        /**
         * 将socketchannel设置为非阻塞工作模式
         */
        socketChannel.configureBlocking(false);

        /**
         * 将channel注册到selector上，监听 可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 回复客户端提示信息
         */
        socketChannel.write(Charset.forName("UTF-8").encode("你与聊天室里的其他人都不是朋友关系，请注意隐私安全"));
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
        String request = "";
        while (socketChannel.read(byteBuffer) > 1) {
            /**
             * 切换buffer为读模式
             */
            byteBuffer.flip();

            /**
             * 读取buffer中的内容
             */
            request += Charset.forName("UTF-8").decode(byteBuffer);
        }

        /**
         * 将channel再次注册到selector上，监听他的可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 将客户端发送的请求信息 广播给其他客户端
         */
        if (request.length() > 0) {
            //广播给其他客户端
            System.out.println("：：" + request);
            broadCast(selector, socketChannel, request);
        }
    }

    /**
     * 广播给其他客户端
     */
    private void broadCast(Selector selector, SocketChannel sourceChannel, String request) {
        /**
         * 获取到所有已接入的客户端channel
         */
        Set<SelectionKey> selectionKeySet = selector.keys();
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();
            if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {
                try {
                    //将信息发送到targetChannel 客户端
                    ((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(request));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * 循环向所有channel广播信息
         */
    }

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer();
        nioServer.start();
    }
}
