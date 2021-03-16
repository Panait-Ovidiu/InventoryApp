package com.example.ovidiu.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ovidiu.inventoryapp.data.ProductContract;

public class ProductCursorAdapter extends CursorAdapter {

    Context mContext;

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param cursor  The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor cursor, boolean autoReQuery) {
        super(context, cursor, autoReQuery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate and return a new view without binding any data
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list_item layout
        TextView productName = (TextView) view.findViewById(R.id.name);
        TextView productPrice = (TextView) view.findViewById(R.id.price);
        TextView productQuantity = (TextView) view.findViewById(R.id.quantity);
        ImageView productImage = (ImageView) view.findViewById(R.id.image);

        // Read the product attributes from the Cursor for the current pet
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QUANTITY));
        byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE));
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        }
        final Uri uri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI,
                cursor.getInt(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry._ID)));

        productName.setText(name);
        productPrice.setText(context.getString(R.string.currency) + " " + price);
        productQuantity.setText(quantity + " " + context.getString(R.string.availableStock));

        // Sale button
        Button saleButton = (Button) view.findViewById(R.id.button_sale);
        // Listener to button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 0) {
                    int newQuantity = quantity - 1;
                    ContentValues values = new ContentValues();
                    values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, newQuantity);
                    // Update database
                    context.getContentResolver().update(uri, values, null, null);
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_outOfStock), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}