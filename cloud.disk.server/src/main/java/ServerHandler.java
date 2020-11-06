import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE,
        USERNAME_LENGTH,
        USERNAME,
        PASSWORD_HASH_LENGTH,
        PASSWORD_HASH_VALUE,
        FILENAME_LENGTH,
        FILENAME,
        FILE_LENGTH,
        FILE_CONTENT
    }

    private State currentState = State.IDLE;
    private String userName;
    private String passwordHash;
    private int nextPartLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {

            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                if (readed == (byte) SignalByte.AUTHORIZATION.getValue()) {
                    currentState = State.USERNAME_LENGTH;
                    System.out.println("STATE: Waiting for username length");
                } else if (readed == (byte) SignalByte.FILE.getValue()) {
                    currentState = State.FILENAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Waiting for incoming file");
                } else {
                    System.out.println("ERROR: Que te pasa, oye! Mira lo que me mandas - " + readed);
                }
            }

            if (currentState == State.USERNAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    nextPartLength = buf.readInt();
                    currentState = State.USERNAME;
                    System.out.printf("STATE: Username length is %d bytes%n", nextPartLength);
                }
            }

            if (currentState == State.USERNAME) {
                if (buf.readableBytes() >= nextPartLength) {
                    byte[] userNameRaw = new byte[nextPartLength];
                    buf.readBytes(userNameRaw);
                    userName = new String(userNameRaw, "UTF-8");
                    System.out.printf("STATE: Surprise, m****fucker: %s%n", userName);
                    currentState = State.PASSWORD_HASH_LENGTH;
                }
            }

            if (currentState == State.PASSWORD_HASH_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    nextPartLength = buf.readInt();
                    currentState = State.PASSWORD_HASH_VALUE;
                    System.out.printf("STATE: Password hash length is %d bytes%n", nextPartLength);
                }
            }

            if (currentState == State.PASSWORD_HASH_VALUE) {
                if (buf.readableBytes() >= nextPartLength) {
                    byte[] passwordHashRaw = new byte[nextPartLength];
                    buf.readBytes(passwordHashRaw);
                    passwordHash = new String(passwordHashRaw, "UTF-8");
                    System.out.printf("STATE: El hash de tu clave: %s%n", passwordHash);

                    //boolean userFound = Users.exists(userName, passwordHash);
                    boolean userFound = true;
                    if (userFound) {
                        ByteBuf content = Unpooled.directBuffer(2);
                        content.writeByte(SignalByte.AUTHORIZATION.getValue());
                        content.writeBoolean(userFound);
                        try {
                            ctx.writeAndFlush(content).sync();
                        } catch (InterruptedException e) {
                            System.out.printf("Te has roto el servidor! %s%n", e.getMessage());
                        }
                    }
                    currentState = State.IDLE;
                }
            }

//            if (currentState == State.FILE_LENGTH) {
//                if (buf.readableBytes() >= 8) {
//                    fileLength = buf.readLong();
//                    System.out.println("STATE: File length received - " + fileLength);
//                    currentState = State.FILE_CONTENT;
//                }
//            }
//
//            if (currentState == State.FILE_CONTENT) {
//                while (buf.readableBytes() > 0) {
//                    out.write(buf.readByte());
//                    receivedFileLength++;
//                    if (fileLength == receivedFileLength) {
//                        currentState = State.IDLE;
//                        System.out.println("File received");
//                        out.close();
//                        break;
//                    }
//                }
//            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
