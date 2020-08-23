package game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

// 通过这个类处理 websocket 的相关通信逻辑
@ServerEndpoint(value="/game/{userId}")
public class GameAPI {
    // 这个类就表示服务器收到的 websocket 请求
    static class Request {
        public String type;
        public int userId;
        public String roomId; // 使用 String 来表示. 更容易生成唯一的 roomId
        public int row;
        public int col;

        @Override
        public String toString() {
            return "Request{" +
                    "type='" + type + '\'' +
                    ", userId=" + userId +
                    ", roomId='" + roomId + '\'' +
                    ", row=" + row +
                    ", col=" + col +
                    '}';
        }
    }

    private int userId;

    @OnOpen
    public void onOpen(@PathParam("userId") String userIdStr, Session session) {
        userId = Integer.parseInt(userIdStr);
        System.out.println("玩家建立连接: " + userId);

        // 把玩家加入到在线玩家列表中
        OnlineUserManager.getInstance().online(userId, session);
    }

    @OnClose
    public void onClose() {
        System.out.println("玩家断开连接: " + userId);

        // 把玩家从在线玩家列表中剔除
        OnlineUserManager.getInstance().offline(userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("玩家断开连接: " + userId);
        // 把玩家从在线玩家列表中剔除
        OnlineUserManager.getInstance().offline(userId);

        error.printStackTrace();
    }

    // onMessage 收到的数据可能是匹配请求, 也可能是落子请求
    // 根据请求的 type 类型来做出区分,
    // 如果 type 是 startMatch, 处理匹配请求
    // 如果 type 是 putChess, 处理落子请求
    // message 请求内容是一个 JSON 结构的字符串, 于是就需要针对这里的 JSON
    // 进行解析.
    // 解析 JSON 格式
    // Jackson, Gson, fastjson.....
    // 使用 google 出品的 Gson.
    @OnMessage
    public void onMessage(String message, Session session) throws InterruptedException, IOException {
        System.out.printf("收到玩家 %d 的消息: %s\n", userId, message);

        // 实例化 Gson 对象
        Gson gson = new GsonBuilder().create();
        // 把 message 的 JSON 格式的字符串转成了 Request 对象
        Request request = gson.fromJson(message, Request.class);
        if (request.type.equals("startMatch")) {
            // 执行匹配逻辑
            // 引入新的数据结构, 一个匹配队列.
            Matcher.getInstance().addMatchQueue(request);
        } else if (request.type.equals("putChess")) {
            // 执行落子逻辑
            Room curRoom = RoomManager.getInstance().getRoom(request.roomId);
            curRoom.putChess(request);
        } else {
            System.out.println("非法的 type 值! " + request.type);
        }
    }
}
