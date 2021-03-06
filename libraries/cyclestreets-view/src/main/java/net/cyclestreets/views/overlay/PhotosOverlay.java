package net.cyclestreets.views.overlay;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.MotionEvent;

import net.cyclestreets.DisplayPhotoActivity;
import net.cyclestreets.PhotoUploadActivity;
import net.cyclestreets.api.Photo;
import net.cyclestreets.api.PhotoMarkers;
import net.cyclestreets.api.Photos;
import net.cyclestreets.routing.Route;
import net.cyclestreets.view.R;
import net.cyclestreets.views.CycleMapView;

public class PhotosOverlay extends LiveItemOverlay<PhotosOverlay.PhotoItem>
                           implements ButtonTapListener
{
	static public class PhotoItem extends OverlayItem 
	{
		private final Photo photo_;
		private final PhotoMarkers photoMarkers;
		
		public PhotoItem(final Photo photo, final PhotoMarkers photoMarkers) 
		{
			super(photo.id() + "", photo.caption(), photo.position());
			photo_ = photo;
			this.photoMarkers = photoMarkers;
		} // PhotoItem

		public Photo photo() { return photo_; }
		
		// Markers
		@Override
		public Drawable getMarker(int stateBitset) 
		{ 
			return photoMarkers.getMarker(photo_.feature());	
		} // getMarker

		// Equality testing
		@Override
		public int hashCode() { return ((photo_ == null) ? 0 : photo_.id()); }
		
		/*
		 * PhotoItems are equal if underlying Photos have the same id
		 */
		@Override
		public boolean equals(final Object obj) 
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final PhotoItem other = (PhotoItem) obj;
			if (photo_ == null) 
				return (other.photo_ == null);

			return (photo_.id() == other.photo_.id());
		} // equals

		@Override
		public String toString() 
		{
			return "PhotoItem [photo=" + photo_ + "]";
		} // toString	
	} // class PhotoItem

	/////////////////////////////////////////////////////
	private final Context context_;
	private final PhotoMarkers photoMarkers_;

  private final int offset_;
  private final float radius_;
  private final OverlayButton addPhotoBtn_;
	
	public PhotosOverlay(final Context context,
	                     final CycleMapView mapView)
	{
		super(context, 
			    mapView, 
			    null,
			    true);
	
		context_ = context;
		photoMarkers_ = new PhotoMarkers(context.getResources());

    offset_ = DrawingHelper.offset(context);
    radius_ = DrawingHelper.cornerRadius(context);

    final Resources res = context.getResources();
    addPhotoBtn_ = new OverlayButton(res.getDrawable(R.drawable.ic_menu_takephoto),
                                     offset_,
                                     offset_*2,
                                     radius_);
    addPhotoBtn_.bottomAlign();

  } // PhotoItemOverlay

	///////////////////////////////////////////////////
  @Override
  public boolean onButtonTap(final MotionEvent event) {
    if(addPhotoBtn_.hit(event)) {
      context_.startActivity(new Intent(context_, PhotoUploadActivity.class));
      return true;
    } // if ...
    return false;
  } // onButtonTap

  @Override
  public boolean onButtonDoubleTap(final MotionEvent event) {
    return onButtonTap(event);
  } // onButtonDoubleTap

  @Override
  public void drawButtons(final Canvas canvas, final MapView mapView) {
    addPhotoBtn_.draw(canvas);
  } // drawButtons

  ///////////////////////////////////////////////////
	@Override
  protected boolean onItemSingleTap(final int index, final PhotoItem item, final MapView mapView) 
  {
    showPhoto(item, mapView);
    return true;
  } // onItemSingleTap
  
  @Override
  protected boolean onItemDoubleTap(final int index, final PhotoItem item, final MapView mapView) 
  {
    showPhoto(item, mapView);
    return true;
  } // onItemDoubleTap

  private void showPhoto(final PhotoItem item, final MapView mapView)
  {
    final Intent intent = new Intent(context_, DisplayPhotoActivity.class);
    intent.setData(Uri.parse(item.photo().thumbnailUrl()));
    intent.putExtra("caption", item.photo().caption());
    intent.putExtra("url", item.photo().url());
    mapView.getContext().startActivity(intent);
  } // showPhoto

  ///////////////////////////////////////////////////
  protected boolean fetchItemsInBackground(final IGeoPoint mapCentre,
                                           final int zoom,
                                           final BoundingBoxE6 boundingBox)
  {
		GetPhotosTask.fetch(this, mapCentre, zoom, boundingBox);
		return true;
	} // refreshPhotos
	
	/////////////////////////////////////////////////////
	/////////////////////////////////////////////////////
	static private class GetPhotosTask extends AsyncTask<Object,Void,Photos> 
	{
		static void fetch(final PhotosOverlay overlay, 
						          final Object... params)
		{
			new GetPhotosTask(overlay).execute(params);
		} // fetch
		
		//////////////////////////////////////////////////////
		private final PhotosOverlay overlay_;
		
		private  GetPhotosTask(final PhotosOverlay overlay)
		{
			overlay_ = overlay;
		} // GetPhotosTask
		
		protected Photos doInBackground(Object... params) 
		{
			final IGeoPoint mapCentre = (IGeoPoint)params[0];
			int zoom = (Integer) params[1];
			final BoundingBoxE6 boundingBox = (BoundingBoxE6)params[2];

			try {
				return Photos.load(mapCentre, zoom, boundingBox);
			} 
			catch (final Exception ex) {
				// never mind, eh?
			}
			return null;
		} // doInBackground
		
		@Override
		protected void onPostExecute(final Photos photos) 
		{
			final List<PhotosOverlay.PhotoItem> items = new ArrayList<PhotosOverlay.PhotoItem>();
			
			if(photos != null)
				for (final Photo photo: photos) 
					items.add(new PhotosOverlay.PhotoItem(photo, overlay_.photoMarkers_));
			
			overlay_.setItems(items);
		} // onPostExecute
	} // GetPhotosTask
} // class PhotoItemOverlay
