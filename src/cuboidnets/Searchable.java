package cuboidnets;

import cuboidnets.search.State;
import cuboidnets.structure.Tile;
import cuboidnets.structure.TileLink;

import java.awt.image.BufferedImage;
import java.util.Collection;

public interface Searchable {

    Collection<TileLink> getRoots();

    Collection<Tile> getTiles();

    BufferedImage render(State state);
}
