package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.indexing;

import com.telenav.kivakit.collections.map.MultiMap;
import com.telenav.kivakit.core.language.collections.list.ObjectList;
import com.telenav.kivakit.core.language.collections.list.StringList;
import com.telenav.kivakit.language.count.Count;
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

    public TagIndex(TagIndexRequest request)
    {
        this.request = request;
    }

    public EdgeSet edges(String key, String value)
    {
        var identifiers = this.identifiers.get(key + "=" + value);
        if (identifiers != null)
        {
            if (identifiers.size() <= 1_000)
            {
                var edges = new EdgeSet();
                for (var identifier : identifiers)
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
        return StringList.stringList(keyToValues.keySet()).sorted();
    }

    public Count valueCount()
    {
        return Count.count(keyToValues.totalValues());
    }

    public synchronized ObjectList<String> values(String key)
    {
        return keyToValues.get(key);
    }
}
