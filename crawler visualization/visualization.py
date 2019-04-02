import database
import plotly.plotly as py
import plotly.graph_objs as go
import networkx as nx


def prepare_graph():
    page_id = 34197
    edges = []
    pages = database.get_linked_pages_for_page(page_id)
    for page1 in pages:
        pages2 = database.get_linked_pages_for_page(page1)
        print(page1)
        for page2 in pages2:
            edges.append((page1, page2))
        edges.append((page_id, page1))
    G = nx.Graph()
    G.add_edges_from(edges)
    pos = nx.kamada_kawai_layout(G)

    return G, pos


def draw_graph(G, pos):
    lines = go.Scatter(x=[],
                       y=[],
                       mode='lines',
                       line=dict(color='rgb(210,210,210)', width=1),
                       hoverinfo='none'
                       )
    dots = go.Scatter(x=[],
                      y=[],
                      mode='markers+text',
                      name='',
                      marker=dict(symbol='circle',
                                  size=10,
                                  color='#6175c1',
                                  line=dict(color='rgb(50,50,50)', width=1)
                                  ),
                      text=[],
                      opacity=0.8
                      )
    for edge in G.edges():
        if edge[0] in pos.keys() and edge[1] in pos.keys():
            x0, y0 = pos[edge[0]]
            x1, y1 = pos[edge[1]]
            lines['x'] += tuple([x0, x1, None])
            lines['y'] += tuple([y0, y1, None])

    for node in G.nodes():
        x, y = pos[node]
        dots['x'] += tuple([x])
        dots['y'] += tuple([y])

    fig = go.Figure(data=[lines, dots],
                    layout=go.Layout(
                        title='<br>Vizualizacija preiskovanja v širino na začetni strani http://evem.gov.si; globina: 2',
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

    py.plot(fig, filename='vizualizacija_preiskovanja')


if __name__ == '__main__':
    G, pos = prepare_graph()
    draw_graph(G, pos)

