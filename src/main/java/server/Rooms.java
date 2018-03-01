package server;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class Rooms
{
    static private List<Game> GameObjs;

    Rooms()
    {
        GameObjs = new ArrayList<Game>();
        GameObjs.add(new Game());
    }

    private Game FindRoom(ChannelHandlerContext ctx)
    {

        for (Game GameObj: GameObjs)
        {
            if (GameObj.isHere(ctx))
                return (GameObj);
        }
        return (null);
    }

    public void NewPlayer(ChannelHandlerContext ctx)
    {
        Game    room = null;

        for (Game GameObj: GameObjs)
        {
            if (!GameObj.isBegin)
            {
                room = GameObj;
                break;
            }
        }
        if (room == null)
        {
            room = new Game();
            GameObjs.add(room);
        }
        room.AddPlayer(ctx);
    }

    public void UseMsg(ChannelHandlerContext ctx, String msg)
    {
        Game    room;

        room = FindRoom(ctx);
        if (room == null)
            return ;
        room.ParseMsg(ctx, msg);
    }

    public void RemovePlayer(ChannelHandlerContext ctx)
    {
        Game    room;

        room = FindRoom(ctx);
        if (room == null)
            return ;
        room.PlayerLeave(ctx);
    }
}
