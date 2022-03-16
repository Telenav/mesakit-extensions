package com.telenav.mesakit.plugins.josm.graph.view.tabs.search;

import com.telenav.kivakit.core.string.StringTo;
import com.telenav.kivakit.core.string.Strings;

/**
 * @author jonathanl (shibo)
 */
public class UserFeedback
{
    public static UserFeedback html(String message, Object... arguments)
    {
        return new UserFeedback().withHtml(message, arguments);
    }

    public static UserFeedback status(String message, Object... arguments)
    {
        return new UserFeedback().withStatus(message, arguments);
    }

    public static UserFeedback text(String message, Object... arguments)
    {
        return new UserFeedback().withText(message, arguments);
    }

    String status;

    String text;

    String html;

    private UserFeedback()
    {
    }

    private UserFeedback(UserFeedback that)
    {
        status = that.status;
        text = that.text;
        html = that.html;
    }

    public String html()
    {
        return html;
    }

    public String status()
    {
        return status;
    }

    public String text()
    {
        return text;
    }

    public UserFeedback withHtml(String message, Object... arguments)
    {
        var copy = new UserFeedback(this);
        copy.html = Strings.format(message, arguments);
        return copy;
    }

    public UserFeedback withStatus(String message, Object... arguments)
    {
        var copy = new UserFeedback(this);
        copy.status = Strings.format(message, arguments);
        return copy;
    }

    public UserFeedback withText(String message, Object... arguments)
    {
        var copy = new UserFeedback(this);
        copy.text = StringTo.html(Strings.format(message, arguments));
        return copy;
    }
}
