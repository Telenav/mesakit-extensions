package com.telenav.tdk.josm.plugins.graph.view.tabs.search;

import com.telenav.tdk.core.kernel.language.string.Strings;
import com.telenav.tdk.core.kernel.messaging.Message;

/**
 * @author jonathanl (shibo)
 */
public class UserFeedback
{
    public static UserFeedback html(final String message, final Object... arguments)
    {
        return new UserFeedback().withHtml(message, arguments);
    }

    public static UserFeedback status(final String message, final Object... arguments)
    {
        return new UserFeedback().withStatus(message, arguments);
    }

    public static UserFeedback text(final String message, final Object... arguments)
    {
        return new UserFeedback().withText(message, arguments);
    }

    String status;

    String text;

    String html;

    private UserFeedback()
    {
    }

    private UserFeedback(final UserFeedback that)
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

    public UserFeedback withHtml(final String message, final Object... arguments)
    {
        final var copy = new UserFeedback(this);
        copy.html = Message.format(message, arguments);
        return copy;
    }

    public UserFeedback withStatus(final String message, final Object... arguments)
    {
        final var copy = new UserFeedback(this);
        copy.status = Message.format(message, arguments);
        return copy;
    }

    public UserFeedback withText(final String message, final Object... arguments)
    {
        final var copy = new UserFeedback(this);
        copy.text = Strings.toHtml(Message.format(message, arguments));
        return copy;
    }
}
