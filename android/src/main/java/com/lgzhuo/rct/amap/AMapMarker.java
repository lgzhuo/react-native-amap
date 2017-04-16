package com.lgzhuo.rct.amap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.FloatRange;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import javax.annotation.Nullable;

/**
 * Created by lgzhuo on 2017/3/9.
 */

public class AMapMarker extends AMapFeature implements View.OnClickListener {

    private Marker marker;
    private MarkerOptions markerOptions = new MarkerOptions();
    private AMapCallout callout;
    private View wrappedCallout;
    private float markerHue = 0.0f; // should be between 0 and 330
    private int width;
    private int height;
    private Bitmap iconBitmap;
    private BitmapDescriptor iconBitmapDescriptor;
    private DataSource<CloseableReference<CloseableImage>> dataSource;
    private final DraweeHolder<?> logoHolder;
    private final ControllerListener<ImageInfo> mLogoControllerListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(
                        String id,
                        @Nullable final ImageInfo imageInfo,
                        @Nullable Animatable animatable) {
                    CloseableReference<CloseableImage> imageReference = null;
                    try {
                        imageReference = dataSource.getResult();
                        if (imageReference != null) {
                            CloseableImage image = imageReference.get();
                            if (image != null && image instanceof CloseableStaticBitmap) {
                                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
                                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                if (bitmap != null) {
                                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    iconBitmap = bitmap;
                                    iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                                }
                            }
                        }
                    } finally {
                        dataSource.close();
                        if (imageReference != null) {
                            CloseableReference.closeSafely(imageReference);
                        }
                    }
                    updateIcon();
                }
            };

    public AMapMarker(Context context) {
        super(context);
        logoHolder = DraweeHolder.create(createDraweeHierarchy(), context);
        logoHolder.onAttach();
    }

    private GenericDraweeHierarchy createDraweeHierarchy() {
        return new GenericDraweeHierarchyBuilder(getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .setFadeDuration(0)
                .build();
    }

    @Override
    public void setAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        super.setAlpha(alpha);
        markerOptions.alpha(alpha);
        if (marker != null) {
            marker.setAlpha(alpha);
        }
//        update();
    }

    public void setAnchor(float u, float v) {
        markerOptions.anchor(u, v);
        if (marker != null) {
            marker.setAnchor(u, v);
        }
//        update();
    }

    public void setDraggable(boolean draggable) {
        markerOptions.draggable(draggable);
        if (marker != null) {
            marker.setDraggable(draggable);
        }
//        update();
    }

    public void setInfoWindowEnable(boolean infoWindowEnable) {
        markerOptions.infoWindowEnable(infoWindowEnable);
        if (marker != null) {
            marker.setInfoWindowEnable(infoWindowEnable);
        }
//        update();
    }

    public void setPosition(LatLng latLng) {
        markerOptions.position(latLng);
        if (marker != null) {
            marker.setPosition(latLng);
        }
//        update();
    }

    public void setRotateAngle(float rotate) {
        markerOptions.rotateAngle(rotate);
        if (marker != null) {
            marker.setRotateAngle(rotate);
        }
//        update();
    }

    public void setFlat(boolean flat) {
        markerOptions.setFlat(flat);
        if (marker != null) {
            marker.setFlat(flat);
        }
//        update();
    }

    public void setGps(boolean gps) {
        markerOptions.setGps(gps);
//        update();
    }

    public void setInfoWindowOffset(int offsetX, int offsetY) {
        markerOptions.setInfoWindowOffset(offsetX, offsetY);
//        update();
    }

    public void setSnippet(String snippet) {
        markerOptions.snippet(snippet);
        if (marker != null) {
            marker.setSnippet(snippet);
        }
//        update();
    }

    public void setTitle(String title) {
        markerOptions.title(title);
        if (marker != null) {
            marker.setTitle(title);
        }
//        update();
    }

    public void setVisible(boolean visible) {
        markerOptions.visible(visible);
        if (marker != null) {
            marker.setVisible(visible);
        }
//        update();
    }

    public void setZIndex(float zIndex) {
        markerOptions.zIndex(zIndex);
        if (marker != null) {
            marker.setZIndex(zIndex);
        }
//        update();
    }

    public void setMarkerHue(float markerHue) {
        this.markerHue = markerHue;
        updateIcon();
    }

    public void setImage(String uri) {
        if (TextUtils.isEmpty(uri)) {
            iconBitmapDescriptor = null;
            update();
        } else if (uri.startsWith("http://") || uri.startsWith("https://") ||
                uri.startsWith("file://")) {
            ImageRequest imageRequest = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(uri))
                    .build();

            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setControllerListener(mLogoControllerListener)
                    .setOldController(logoHolder.getController())
                    .build();
            logoHolder.setController(controller);
        } else {
            iconBitmapDescriptor = getBitmapDescriptorByName(uri);
            updateIcon();
        }
    }

    public void update(int width, int height) {
        this.width = width;
        this.height = height;
        updateIcon();
    }

    public void updateIcon() {
        markerOptions.icon(getIcon());
        update();
    }

    public void update() {
        if (marker != null) {
//            marker.setMarkerOptions(markerOptions);
            marker.setIcon(markerOptions.getIcon());
        }
    }

    public void setcallout(AMapCallout callout) {
        if (this.callout != callout) {
            if (this.wrappedCallout != null) {
                this.wrappedCallout.setOnClickListener(null);
                this.wrappedCallout = null;
            }
        }
        this.callout = callout;
    }

    private BitmapDescriptor getIcon() {
        if (getChildCount() > 0) {
            // creating a bitmap cnv an arbitrary view
            if (iconBitmapDescriptor != null) {
                Bitmap viewBitmap = createDrawable();
                int width = Math.max(iconBitmap.getWidth(), viewBitmap.getWidth());
                int height = Math.max(iconBitmap.getHeight(), viewBitmap.getHeight());
                Bitmap combinedBitmap = Bitmap.createBitmap(width, height, iconBitmap.getConfig());
                Canvas canvas = new Canvas(combinedBitmap);
                canvas.drawBitmap(iconBitmap, 0, 0, null);
                canvas.drawBitmap(viewBitmap, 0, 0, null);
                return BitmapDescriptorFactory.fromBitmap(combinedBitmap);
            } else {
                return BitmapDescriptorFactory.fromBitmap(createDrawable());
            }
        } else if (iconBitmapDescriptor != null) {
            // use local image as a marker
            return iconBitmapDescriptor;
        } else {
            // render the default marker pin
            return BitmapDescriptorFactory.defaultMarker(this.markerHue);
        }
    }

    private Bitmap createDrawable() {
        int width = this.width <= 0 ? 100 : this.width;
        int height = this.height <= 0 ? 100 : this.height;
        this.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        this.draw(canvas);

        return bitmap;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child instanceof TextureView) {
            TextureView surfaceView = (TextureView) child;
            if (AMapUtils.drawSurfaceView(canvas, surfaceView)) {
                return true;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void addToMap(AMap map) {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
        marker = map.addMarker(markerOptions);
    }

    @Override
    public void removeFromMap(AMap map) {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    @Override
    public Marker getFeature() {
        return marker;
    }

    public View getCustomInfo() {
        return null;
    }

    public View getInfoWindow() {
        if (this.callout == null) return null;

        if (this.wrappedCallout == null) {
            this.wrapCalloutView();
        }

        if (this.wrappedCallout != null) {
            if (this.callout.isTooltip()) {
                this.wrappedCallout.setBackgroundColor(Color.TRANSPARENT);
            } else {
                this.wrappedCallout.setBackground(null);
            }
        }
        return this.wrappedCallout;
    }

    public View getInfoContents() {
//        if (this.callout == null) return null;
//
//        if (this.wrappedCallout == null) {
//            this.wrapCalloutView();
//        }
//
//        if (this.callout.isTooltip()) {
//            return null;
//        } else {
//            return this.wrappedCallout;
//        }
        return null;
    }

    private void wrapCalloutView() {
        if (this.callout == null || this.callout.getChildCount() == 0) {
            return;
        }

        FrameLayout FL = new FrameLayout(getContext());
        FL.setLayoutParams(new ViewGroup.LayoutParams(
                this.callout.width,
                this.callout.height
        ));

        FL.addView(this.callout);
        FL.setOnClickListener(this);

        this.wrappedCallout = FL;
    }

    private int getDrawableResourceByName(String name) {
        return getResources().getIdentifier(
                name,
                "drawable",
                getContext().getPackageName());
    }

    private BitmapDescriptor getBitmapDescriptorByName(String name) {
        return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
    }

    /* Events */
    public void pushEvent(String eventName, WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), eventName, event);
    }

    public void pushOnPressEvent() {
        pushEvent("onPress", null);
    }

    public void pushOnCalloutPressEvent() {
        pushEvent("onCalloutPress", null);
    }

    /*  View.OnClickListener */

    @Override
    public void onClick(View view) {
        pushOnCalloutPressEvent();
    }
}
