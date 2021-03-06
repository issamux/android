package net.cyclestreets.views;

import java.util.Map;

import net.cyclestreets.CycleStreetsPreferences;
import net.cyclestreets.view.R;
import net.cyclestreets.util.MapFactory;
import net.cyclestreets.util.MapPack;
import net.cyclestreets.util.MessageBox;
import net.cyclestreets.views.overlay.LocationOverlay;
import net.cyclestreets.views.overlay.ControllerOverlay;
import net.cyclestreets.views.overlay.ZoomButtonsOverlay;

import org.mapsforge.android.maps.MapsforgeOSMDroidTileProvider;
import org.mapsforge.android.maps.MapsforgeOSMTileSource;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.location.Location;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;

public class CycleMapView extends MapView
{
  private static final String PREFS_APP_CENTRE_LON = "centreLon";
  private static final String PREFS_APP_CENTRE_LAT = "centreLat";
  private static final String PREFS_APP_ZOOM_LEVEL = "zoomLevel";
  private static final String PREFS_APP_MY_LOCATION = "myLocation";
  private static final String PREFS_APP_FOLLOW_LOCATION = "followLocation";

  private ITileSource renderer_;
  private final SharedPreferences prefs_;
  private final ControllerOverlay controllerOverlay_;
  private final LocationOverlay location_;
  private final int overlayBottomIndex_;

  private GeoPoint centreOn_ = null;

  public CycleMapView(final Context context, final String name) {
    super(context,
          256,
          new DefaultResourceProxyImpl(context),
          mapTileProvider(context));

    prefs_ = context.getSharedPreferences("net.cyclestreets.mapview."+name, Context.MODE_PRIVATE);

    setBuiltInZoomControls(false);
    setMultiTouchControls(true);

    overlayBottomIndex_ = getOverlays().size();

    getOverlays().add(new ZoomButtonsOverlay(context, this));

    location_ = new LocationOverlay(context, this);
    getOverlays().add(location_);

    controllerOverlay_ = new ControllerOverlay(context, this);
    getOverlays().add(controllerOverlay_);
  } // CycleMapView

  public Overlay overlayPushBottom(final Overlay overlay)
  {
    getOverlays().add(overlayBottomIndex_, overlay);
    return overlay;
  } // overlayPushBottom

  public Overlay overlayPushTop(final Overlay overlay)
  {
    // keep TapOverlay on top
    int front = getOverlays().size()-1;
    getOverlays().add(front, overlay);
    return overlay;
  } // overlayPushFront

  /////////////////////////////////////////
  // save/restore
  public void onPause()
  {
    final SharedPreferences.Editor edit = prefs_.edit();
    final IGeoPoint centre = getMapCenter();
    int lon = centre.getLongitudeE6();
    int lat = centre.getLatitudeE6();
    edit.putInt(PREFS_APP_CENTRE_LON, lon);
    edit.putInt(PREFS_APP_CENTRE_LAT, lat);
    edit.putInt(PREFS_APP_ZOOM_LEVEL, getZoomLevel());
    edit.putBoolean(PREFS_APP_MY_LOCATION, location_.isMyLocationEnabled());
    edit.putBoolean(PREFS_APP_FOLLOW_LOCATION, location_.isFollowLocationEnabled());

    disableMyLocation();

    controllerOverlay_.onPause(edit);
    edit.commit();
  } // onPause

  public void onResume()
  {
    final ITileSource tileSource = mapRenderer();
    if(!tileSource.equals(renderer_))
    {
      renderer_ = tileSource;
      setTileSource(renderer_);
    } // if ...

    location_.enableLocation(pref(PREFS_APP_MY_LOCATION, true));
    if(pref(PREFS_APP_FOLLOW_LOCATION, true))
      location_.enableFollowLocation();
    else
      location_.disableFollowLocation();

    if(centreOn_ == null) {
      // mild data race if we're setting centre in onActivityResult
      // because that's followed by an onResume
      int lon = pref(PREFS_APP_CENTRE_LON, 0);
      int lat = pref(PREFS_APP_CENTRE_LAT, 51477841); /* Greenwich */
      final GeoPoint centre = new GeoPoint(lat, lon);
      getScroller().abortAnimation();
      getController().setCenter(centre);
      centreOn(centre);
    } // if ...

    getController().setZoom(pref(PREFS_APP_ZOOM_LEVEL, 14));

    controllerOverlay_.onResume(prefs_);
  } // onResume

  ////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////
  public void onCreateOptionsMenu(final Menu menu)
  {
    controllerOverlay_.onCreateOptionsMenu(menu);
  } // onCreateOptionsMenu

  public void onPrepareOptionsMenu(final Menu menu)
  {
    controllerOverlay_.onPrepareOptionsMenu(menu);
  } // onPrepareOptionsMenu

  public boolean onMenuItemSelected(final int featureId, final MenuItem item)
  {
    return controllerOverlay_.onMenuItemSelected(featureId, item);
  } // onMenuItemSelected

  @Override
  public void onCreateContextMenu(final ContextMenu menu)
  {
    controllerOverlay_.onCreateContextMenu(menu);
  } //  onCreateContextMenu

  public boolean onBackPressed()
  {
    return controllerOverlay_.onBackPressed();
  } // onBackPressed

  /////////////////////////////////////////
  // location
  public Location getLastFix() { return location_.getLastFix(); }
  public boolean isMyLocationEnabled() { return location_.isMyLocationEnabled(); }
  public void toggleMyLocation() { location_.enableLocation(!location_.isMyLocationEnabled()); }
  public void disableMyLocation() { location_.disableMyLocation(); }
  public void disableFollowLocation() { location_.disableFollowLocation(); }

  public void enableAndFollowLocation() { location_.enableAndFollowLocation(true); }
  public void lockOnLocation() { location_.lockOnLocation(); }
  public void hideLocationButton() { location_.hideButton(); }

  ///////////////////////////////////////////////////////
  public void centreOn(final GeoPoint place)
  {
    centreOn_ = place;
    invalidate();
  } // centreOn

  @Override
  protected void dispatchDraw(final Canvas canvas)
  {
    if(centreOn_  != null)
    {
      getController().animateTo(new GeoPoint(centreOn_));
      centreOn_ = null;
      return;
    } // if ..

    super.dispatchDraw(canvas);
  } // dispatchDraw

  ///////////////////////////////////////////////////////
  private int pref(final String key, int defValue)
  {
    return prefs_.getInt(key, defValue);
  } // pref
  private boolean pref(final String key, boolean defValue)
  {
    return prefs_.getBoolean(key, defValue);
  } // pref

  public String mapAttribution()
  {
    try {
      return attribution_.get(CycleStreetsPreferences.mapstyle());
    }
    catch(Exception e) {
      // sigh
    } // catch
    return attribution_.get(DEFAULT_RENDERER);
  } // mapAttribution

  private ITileSource mapRenderer()
  {
    try {
      final ITileSource renderer = TileSourceFactory.getTileSource(CycleStreetsPreferences.mapstyle());

      if(renderer instanceof MapsforgeOSMTileSource)
      {
    	final String mapFile = CycleStreetsPreferences.mapfile();
        final MapPack pack = MapPack.findByPackage(mapFile);
        if(pack.current())
          ((MapsforgeOSMTileSource)renderer).setMapFile(mapFile);
        else
        {
          MessageBox.YesNo(this,
                           R.string.out_of_date_map_pack,
                           new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface arg0, int arg1) {
                               MapPack.searchGooglePlay(getContext());
                             } // onClick
                           });
          CycleStreetsPreferences.resetMapstyle();
          return TileSourceFactory.getTileSource(DEFAULT_RENDERER);
        }
      }

      return renderer;
    } // try
    catch(Exception e) {
      // oh dear
    } // catch
    return TileSourceFactory.getTileSource(DEFAULT_RENDERER);
  } // mapRenderer

  static private String DEFAULT_RENDERER = CycleStreetsPreferences.MAPSTYLE_OCM;
  static private Map<String, String> attribution_ =
      MapFactory.map(CycleStreetsPreferences.MAPSTYLE_OCM, "\u00a9 OpenStreetMap and contributors, CC-BY-SA. Map images \u00a9 OpenCycleMap")
                .map(CycleStreetsPreferences.MAPSTYLE_OSM, "\u00a9 OpenStreetMap and contributors, CC-BY-SA")
                .map(CycleStreetsPreferences.MAPSTYLE_OS, "Contains Ordnance Survey Data \u00a9 Crown copyright and database right 2010")
                .map(CycleStreetsPreferences.MAPSTYLE_MAPSFORGE, "\u00a9 OpenStreetMap and contributors, CC-BY-SA");

  static
  {
    final OnlineTileSourceBase OPENCYCLEMAP = new XYTileSource(CycleStreetsPreferences.MAPSTYLE_OCM,
                    ResourceProxy.string.cyclemap, 0, 17, 256, ".png",
                    "http://tile.cyclestreets.net/opencyclemap/");
    final OnlineTileSourceBase OPENSTREETMAP = new XYTileSource(CycleStreetsPreferences.MAPSTYLE_OSM,
                    ResourceProxy.string.base, 0, 17, 256, ".png",
                    "http://tile.cyclestreets.net/mapnik/");
    final OnlineTileSourceBase OSMAP = new XYTileSource(CycleStreetsPreferences.MAPSTYLE_OS,
                    ResourceProxy.string.unknown, 0, 17, 256, ".png",
                    "http://a.os.openstreetmap.org/sv/",
                    "http://b.os.openstreetmap.org/sv/",
                    "http://c.os.openstreetmap.org/sv/");
    final MapsforgeOSMTileSource MAPSFORGE = new MapsforgeOSMTileSource(CycleStreetsPreferences.MAPSTYLE_MAPSFORGE);
    TileSourceFactory.addTileSource(OPENCYCLEMAP);
    TileSourceFactory.addTileSource(OPENSTREETMAP);
    TileSourceFactory.addTileSource(OSMAP);
    TileSourceFactory.addTileSource(MAPSFORGE);
  } // static

  static private MapTileProviderBase mapTileProvider(final Context context)
  {
    return new CycleMapTileProvider(context);
  } // MapTileProviderBase

  static private class CycleMapTileProvider extends MapTileProviderArray
                                            implements IMapTileProviderCallback
  {
    public CycleMapTileProvider(final Context context)
    {
      this(context,
           TileSourceFactory.getTileSource(DEFAULT_RENDERER),
           new SimpleRegisterReceiver(context));
    } // CycleMapTileProvider

    private CycleMapTileProvider(final Context context,
                                 final ITileSource tileSource,
                                 final IRegisterReceiver registerReceiver)
    {
      super(tileSource, registerReceiver);

      final MapTileFilesystemProvider fileSystemProvider =
            new MapTileFilesystemProvider(registerReceiver, tileSource);
      mTileProviderList.add(fileSystemProvider);

      final NetworkAvailabliltyCheck networkCheck = new NetworkAvailabliltyCheck(context);

      final MapTileDownloader downloaderProvider =
            new MapTileDownloader(tileSource,
            					  new TileWriter(),
                                  networkCheck);
      mTileProviderList.add(downloaderProvider);

      final MapsforgeOSMDroidTileProvider mapsforgeProvider =
            new MapsforgeOSMDroidTileProvider(tileSource, networkCheck);
      mTileProviderList.add(mapsforgeProvider);
    } // CycleMapTileProvider
  } // CycleMapTileProvider
} // CycleMapView
