import database
import plotly.plotly as py
import plotly.graph_objs as go
import networkx as nx

data = {}


def prepare_graph():
    edges = []

    for key in data.keys():
        for val in data[key]:
            edges.append((key, val))
    G = nx.Graph()
    G.add_edges_from(edges)
    pos = nx.kamada_kawai_layout(G)

    return G, pos


def draw_graph(G, pos):
    edge_trace = go.Scatter(
        x=[],
        y=[],
        line=dict(width=0.5, color='#888'),
        hoverinfo='none',
        mode='lines')

    for edge in G.edges():
        if edge[0] in pos.keys() and edge[1] in pos.keys():
            x0, y0 = pos[edge[0]]
            x1, y1 = pos[edge[1]]
            edge_trace['x'] += tuple([x0, x1, None])
            edge_trace['y'] += tuple([y0, y1, None])

    node_trace = go.Scatter(
        x=[],
        y=[],
        text=[],
        mode='markers+text', # only marker if you don't want to have always visible text but only on hover
        marker=dict(
            showscale=True,
            # colorscale options
            # 'Greys' | 'YlGnBu' | 'Greens' | 'YlOrRd' | 'Bluered' | 'RdBu' |
            # 'Reds' | 'Blues' | 'Picnic' | 'Rainbow' | 'Portland' | 'Jet' |
            # 'Hot' | 'Blackbody' | 'Earth' | 'Electric' | 'Viridis' |
            colorscale='YlOrRd',
            reversescale=True,
            color=[],
            size=[],
            colorbar=dict(
                thickness=15,
                title='Število strani na spletišče',
                xanchor='left',
                titleside='right'
            ),
            line=dict(width=2)))

    for node in G.nodes():
        x, y = pos[node]
        node_trace['x'] += tuple([x])
        node_trace['y'] += tuple([y])

        pages_count = database.get_pages_count_for_site(node)
        name = database.get_site_by_id(node)[1]
        node_trace['marker']['color'] += tuple([pages_count])
        # node_info = '# of pages: ' + str(pages_count) + "\n " + name
        # node_trace['text'] += tuple([node_info])
        node_trace['text'] += tuple([name])
        node_trace['marker']['size'] += tuple([pages_count / 100])

    fig = go.Figure(data=[edge_trace, node_trace],
                    layout=go.Layout(
                        title='<br>Vizualizacija povezav med spletišči in njihova velikost',
                        titlefont=dict(size=16),
                        showlegend=False,
                        hovermode='closest',
                        margin=dict(b=20, l=5, r=5, t=40),
                        annotations=[dict(
                            text="Python code: <a href='https://plot.ly/ipython-notebooks/network-graphs/'> https://plot.ly/ipython-notebooks/network-graphs/</a>",
                            showarrow=False,
                            xref="paper", yref="paper",
                            x=0.005, y=-0.002)],
                        xaxis=dict(showgrid=False, zeroline=False, showticklabels=False),
                        yaxis=dict(showgrid=False, zeroline=False, showticklabels=False)))

    py.plot(fig, filename='vizualizacija_spletisc')


if __name__ == '__main__':
    data = database.get_connections_between_sites()
    G, pos = prepare_graph()
    draw_graph(G, pos)

