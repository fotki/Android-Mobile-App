package com.tbox.fotki.view.view.zoomable_view;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.tbox.fotki.refactoring.screens.slider.SliderActivity;
import com.tbox.fotki.view.activities.ImageSliderActivity;
import com.tbox.fotki.model.entities.ParcelableItem;

/**
 * Created by Junaid on 4/20/17.
 */

public class TapGestureListner extends DoubleTapGestureListener {
    private Context mContext;
    ParcelableItem mItem;
    public TapGestureListner(ZoomableDraweeView zoomableDraweeView){

        super((ZoomableDraweeView)zoomableDraweeView);
    }
    public void setTapCallback(Context context , ParcelableItem item){
        this.mContext = context;
        this.mItem=item;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (this.mContext != null){
            SliderActivity tapCallback = (SliderActivity)this.mContext;
            tapCallback.singleTapDone(mItem);
        }
        return true;
    }
}
