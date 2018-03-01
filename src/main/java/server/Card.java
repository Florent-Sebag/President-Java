package server;

public class Card
{
    String        index;
    Integer         value;
    Colors      color;

    Card(int _value, Integer _color)
    {
        value = _value;
        color = Colors.values()[_color];
        if (value == 11)
            index = new String("J");
        else if (value == 12)
            index = new String("Q");
        else if (value == 13)
            index = new String("K");
        else if (value == 14)
            index = new String("A");
        else if (value == 15)
            index = new String("2");
        else
            index = value.toString();
    }
}
