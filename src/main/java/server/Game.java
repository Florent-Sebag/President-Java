package server;

import io.netty.channel.ChannelHandlerContext;

import java.util.*;

public class Game
{
    private List<Card>      Deck;
    private List<Player>    Players;
    private List<Card>      MidCards;
    private List<Card>      Playeds;
    public boolean         isBegin = false;
    private int             id_turn;
    private int             nb_cards;
    private int             finished = 0;

    Game()
    {
        Deck = new ArrayList<Card>();
        Players = new ArrayList<Player>();
        MidCards = new ArrayList<Card>();
        Playeds = new ArrayList<Card>();
    }

    public boolean isHere(ChannelHandlerContext ctx)
    {
        for (Player player : Players)
        {
            if (ctx.equals(player.ctx))
                return (true);
        }
        return (false);
    }

    private void ResetGame()
    {
        isBegin = false;
        Deck.clear();
        MidCards.clear();
        Playeds.clear();
        finished = 0;
        id_turn = 0;
        for (Player player: Players)
            player.ResetPlayer();
        SendToAllPlayers("New game, please write 'Ready' when you are or 'bye' if you want to quit.");
    }

    public void PlayerLeave(ChannelHandlerContext ctx)
    {
        Player from = FindPlayer(ctx);

        Players.remove(from);
        SendToAllPlayers("\nA player has left the room, the game starts again !");
        ResetGame();
    }

    public void AddPlayer(ChannelHandlerContext ctx)
    {
        Player ToAdd;
        int    id;

        id = Players.size();
        ToAdd = new Player(ctx, id);
        Players.add(ToAdd);
        ToAdd.SendMsg("New game, you are the player " + ToAdd.id + " ! Please write 'Ready' when you are or 'bye' if you want to quit.");
        SendToAllPlayers("Player " + id + " join the game.", ToAdd);
    }

    private boolean  checkAllReady()
    {
        if (Players.size() < 2)
            return (false);
        for (Player player: Players)
        {
            if (!player.isReady)
                return (false);
        }
        isBegin = true;
        return (true);
    }

    private void InitDeck()
    {
        int i = -1;
        int j = 2;

        while (++i < 4)
        {
            while (++j < 16)
            {
                Card ToAdd = new Card(j, i);
                Deck.add(ToAdd);
            }
            j = 2;
        }
    }

    private void    InitHands()
    {
        Random  r = new Random();
        int     size = Deck.size();
        int     index;
        int     IndexPlayer = 0;
        Card ToAdd;

        while (size > 0)
        {
            index = r.nextInt(size);
            ToAdd = Deck.remove(index);
            Players.get(IndexPlayer).Hand.add(ToAdd);
            size -= 1;
            IndexPlayer += 1;
            if (IndexPlayer == Players.size())
                IndexPlayer = 0;
        }
    }


    private void    InitGame()
    {
        SendToAllPlayers("Everybody is ready, the game starts !");
        InitDeck();
        InitHands();
        id_turn = 0;
        SendHands();
    }

    private int ParseBeginMsg(Player player, String msg)
    {
        if (!msg.equals("Ready"))
            return (-1);
        player.isReady = true;
        SendToAllPlayers("Player " + player.id + " is ready", player);
        if (checkAllReady())
        {
            InitGame();
            return (1);
        }
        return (0);
    }

    private void SendToAllPlayers(String msg, Player except)
    {
        for (Player player: Players)
        {
            if (player != except)
                player.SendMsg(msg);
        }
    }

    private void SendToAllPlayers(String msg)
    {
        SendToAllPlayers(msg, null);
    }


    private Player FindPlayer(ChannelHandlerContext from)
    {
        for (Player player: Players)
        {
            if (player.ctx == from)
                return (player);
        }
        return (null);
    }

    private void SendHands()
    {
        for (Player player: Players)
        {
            player.SendHand();
            if (player.id == id_turn)
                player.SendMsg("Your turn, please write what you want to play like that : INDEX:COLOR[,INDEX:COLOR][,...] or 'Pass'");
            else
                player.SendMsg("Player " + id_turn + " turn.");
        }
    }

    private List<Card> checkHasCard(Player from, String msg)
    {
        String  cards[];
        String  tmp[];
        int     old_size = 0;

        cards = msg.split(",");
        for (String str: cards)
        {

            tmp = str.split(":");
            old_size = Playeds.size();
            for (Card card : from.Hand)
            {
                if (tmp[0].equals(card.index) && tmp[1].equals(card.color.toString()))
                    Playeds.add(card);
            }
            if (old_size == Playeds.size())
            {
                from.SendMsg("You don't have this card(s).");
                return (null);
            }
        }
        return (Playeds);
    }
    //HERE
    private boolean checkThisPlay(Player from)
    {
        if (Playeds.size() > 1)
        {
            int tmp = Playeds.get(0).value;
            for (Card played : Playeds)
            {
                if (played.value != tmp)
                {
                    from.SendMsg("You need to play only same card(s)");
                    return (false);
                }
            }
        }
        if (MidCards.size() == 0)
        {
            nb_cards = Playeds.size();
            return (true);
        }
        if (Playeds.size() != nb_cards)
        {
            from.SendMsg("You need to play " + nb_cards + " card(s)");
            return (false);
        }
        if (Playeds.get(0).value < MidCards.get(MidCards.size() - 1).value)
        {
            from.SendMsg("You can't play this card(s).");
            return (false);
        }
        return (true);
    }

    private boolean checkPassed(Player from, String msg)
    {
        if (msg.equals("Pass"))
        {
            from.isPassed = true;
            return (true);
        }
        return (false);
    }

    private boolean checkAllPassed(Player from)
    {
        int nbPassed = 0;

        for (Player player: Players)
        {
            if (player.isPassed)
                nbPassed += 1;
        }
        if (nbPassed >= Players.size() - 1)
        {
            SendToAllPlayers("Player " + from.id + " pass.", from);
            endTurn(1, from);
            return (true);
        }
        SendToAllPlayers("Player " + from.id + " pass.", from);
        getNextTurn();
        SendHands();
        return (false);
    }

    private boolean CheckMsg(Player from, String msg)
    {
        if (from.id != id_turn)
        {
            from.SendMsg("It's not your turn.");
            return (false);
        }
        if (checkHasCard(from, msg) == null)
            return (false);
        return (checkThisPlay(from));
    }

    private int getNextTurn()
    {
        if (MidCards.size() > 0)
        {
            if (MidCards.get(MidCards.size() - 1).value == 15)
                return (0);
        }

        id_turn += 1;
        if (id_turn == Players.size())
            id_turn = 0;
        while (Players.get(id_turn).isPassed || Players.get(id_turn).isFinished)
        {
            id_turn += 1;
            if (id_turn == Players.size())
                id_turn = 0;
        }
        return (-1);
    }

    private boolean addMidCard()
    {
        int last_value = Playeds.get(0).value;
        int nb = 0;
        Card card;

        MidCards.addAll(Playeds);
        for (ListIterator<Card> it = MidCards.listIterator(MidCards.size()); it.hasPrevious();)
        {
            card = it.previous();
            if (card.value == last_value)
                nb += 1;
            else
                break;
            if (nb == 4)
                break;
        }
        return (nb == 4);
    }

    private void    endTurn(int passed, Player from)
    {
        for (Player player: Players)
        {
            if (passed == 1 && !player.isPassed)
            {
                from = player;
                id_turn = player.id;
            }
            player.isPassed = false;
        }
        MidCards.clear();
        SendToAllPlayers("New Turn !");
        SendHands();
    }

    private boolean Finished(Player from)
    {
        int nb_finished = 0;

        finished += 1;
        SendToAllPlayers("Player " + from.id + " has finished his cards. Gj, he is " + finished + " ! ");
        for (Player player: Players)
        {
            if (player.isFinished)
                nb_finished += 1;
        }
        return (nb_finished == Players.size());
    }

    private int NewTurn(Player from, String msg)
    {
        int     ret;

        Playeds.clear();
        if (checkPassed(from, msg))
        {
            checkAllPassed(from);
            return (0);
        }
        if (!CheckMsg(from, msg))
            return (0);
        if (from.RemoveFromHand(Playeds))
        {
            if (Finished(from))
                return (1);
        }
        SendToAllPlayers("Player " + from.id + " use " + msg);
        if (addMidCard())
            endTurn(0, from);
        else if ((ret = getNextTurn()) > -1)
            endTurn(ret, from);
        else
            SendHands();
        return (0);
    }

    public int ParseMsg(ChannelHandlerContext ctx, String msg)
    {
        Player from;

        from = FindPlayer(ctx);
        if (msg.equals("bye"))
        {
            Players.remove(from);
            SendToAllPlayers("The game is over !");
            ResetGame();
        }
        if (!isBegin)
            return (ParseBeginMsg(from, msg));
        if (NewTurn(from, msg) == 1)
        {
            SendToAllPlayers("The game is over !");
            ResetGame();
        }
        return (0);
    }
}
