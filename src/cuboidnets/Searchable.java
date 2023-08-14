package cuboidnets;

import java.awt.image.BufferedImage;
import java.util.Collection;

public interface Searchable {


    Collection<TileLink> getRoots();

    Collection<Tile> getTiles();

    BufferedImage render(SearchState state);
}
