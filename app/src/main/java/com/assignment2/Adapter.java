package com.assignment2;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.util.List;

/**
 * The grid adapter is responsible for displaying all the images
 * in the users device in the Grid View
 */
public class Adapter extends BaseAdapter {



    private Context mContext; //Reference  to the context
    private List<Uri> bmps; //List of Uris pointing to specific bitmaps

    /**
     *
     * @return Size of adapter
     */
    @Override
    public int getCount() {
        return bmps.size();
    }

    /**
     *
     * @param i Item at index
     * @return Item at index
     */
    @Override
    public Object getItem(int i) {
        return bmps.get(i);
    }

    /**
     *
     * @param i
     * @return 0 since this method is never called.
     */
    @Override
    public long getItemId(int i) {
        return 0;
    }


    /**
     *
     * @param mContext Activity in which gridview will be displayed
     * @param bmps List of Images to display.
     */
    public Adapter(Context mContext, List<Uri> bmps) {
        this.mContext = mContext;
        this.bmps = bmps;
    }

    /**
     *
     * @param i Position of element
     * @param view Reference to the current Vieew
     * @param viewGroup Reference to Node in View
     * @return View
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ImageView imageView; //Load a imageView object
        BitmapFactory.Options opt = new BitmapFactory.Options(); // generate options for all Bitmaps.
        opt.inDither = false; //Turn off sampling
        opt.inPurgeable = true;
        opt.inInputShareable=true;
        if(view == null)
        {
            imageView= new ImageView(mContext); //Set new  ImageView pointing to context
            imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); //Define layout
            imageView.setPadding(10, 10, 10, 10); //Define padding
        }
        else
        {
            imageView = (ImageView) view;
        }

        imageView.setImageURI(bmps.get(i)); //Set Images in File
        imageView.setId(i);
        imageView.setLayoutParams(new GridView.LayoutParams(600, 400)); //Set Dimensions for each Image
        return imageView;


    }

    /**
     *
     * @param bmps Uris to be set
     */
    public void setBmps(List<Uri> bmps) {
        this.bmps = bmps;
    }




}
