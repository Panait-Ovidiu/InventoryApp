package com.example.ovidiu.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.ovidiu.inventoryapp.data.ProductContract;

public class CatalogActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private ProductCursorAdapter mProductCursorAdapter;
    private static final int URI_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        ListView listView = (ListView) findViewById(R.id.list_view);
        View emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);
        mProductCursorAdapter = new ProductCursorAdapter(this, null, false);
        listView.setAdapter(mProductCursorAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                intent.setData(ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        getLoaderManager().initLoader(URI_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case URI_LOADER:
                String projection[] = {
                        ProductContract.ProductEntry._ID,
                        ProductContract.ProductEntry.COLUMN_NAME,
                        ProductContract.ProductEntry.COLUMN_PRICE,
                        ProductContract.ProductEntry.COLUMN_QUANTITY,
                        ProductContract.ProductEntry.COLUMN_IMAGE
                };
                String sortOrder =
                        ProductContract.ProductEntry._ID + " DESC ";
                return new CursorLoader(
                        this,
                        ProductContract.ProductEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        try {
            mProductCursorAdapter.swapCursor(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductCursorAdapter.swapCursor(null);
    }
}

