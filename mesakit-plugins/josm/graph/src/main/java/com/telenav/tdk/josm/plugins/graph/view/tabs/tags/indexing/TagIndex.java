package com.telenav.tdk.josm.plugins.graph.view.tabs.tags.indexing;

import com.telenav.kivakit.collections.map.multi.MultiMap;
import com.telenav.kivakit.kernel.language.collections.list.ObjectList;
import com.telenav.kivakit.kernel.language.string.StringList;
import com.telenav.kivakit.kernel.language.values.Count
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.identifiers.EdgeIdentifier;

/**
 * @author jonathanl (shibo)
 */
public class TagIndex
{
    final MultiMap<String, String> keyToValues = new MultiMap<>();

    final MultiMap<String, EdgeIdentifier> identifiers = new MultiMap<>();

    final TagIndexRequest request;

    public TagIndex(final TagIndexRequest request)
    {
        this.request = request;
    }

    public EdgeSet edges(final String key, final String value)
    {
        final var identifiers = this.identifiers.get(key + "=" + value);
        if (identifiers != null)
        {
            if (identifiers.size() <= 1_000)
            {
                final var edges = new EdgeSet();
                for (final var identifier : identifiers)
                {
                    edges.add(request.graph.edgeForIdentifier(identifier));
                }
                return edges;
            }
        }
        return null;
    }

    public Count keyCount()
    {
        return Count.count(keyToValues.size());
    }

    public synchronized StringList keys()
    {
        return StringList.fromObjects(keyToValues.keySet()).sorted();
    }

    public Count valueCount()
    {
        return Count.count(keyToValues.totalValues());
    }

    public synchronized ObjectList<String> values(final String key)
    {
        return keyToValues.get(key);
    }
}
