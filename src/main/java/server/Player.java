package server;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class Player
{
    ChannelHandlerContext ctx;
    public List<Card>     Hand;
    boolean               isReady = false;
    int                   id;
    boolean               isPassed = false;
    boolean               isFinished = false;

    Player(ChannelHandlerContext _ctx, int _id)
    {
        ctx = _ctx;
        id = _id;
        Hand = new ArrayList<Card>();
    }

    public void ResetPlayer()
    {
        Hand.clear();
        isReady = false;
        isPassed = false;
        isFinished = false;
    }

    public void SendMsg(String msg)
    {
        ctx.channel().writeAndFlush(msg + '\n');
    }

    public void SendHand()
    {
        String toSend = new String();
        Card   last = Hand.get(Hand.size() - 1);

        for (Card card: Hand)
        {
            if (card != last)
                toSend += "[" + card.index + ", " + card.color + "], ";
            else
                toSend += "[" + card.index + ", " + card.color + "]";
        }
        SendMsg(toSend + "\n");
    }

    public boolean RemoveFromHand(List<Card> Playeds)
    {
        for (Card played : Playeds)
        {
            for (Card card : Hand)
            {
                if (card.value.equals(played.value) && card.color.equals(played.color))
                {
                    Hand.remove(card);
                    break;
                }
            }
        }
        if (Hand.size() == 0)
        {
            isFinished = true;
            return (true);
        }
        return (false);
    }
}
