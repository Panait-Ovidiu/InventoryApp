package com.example.ovidiu.inventoryapp;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ovidiu.inventoryapp.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private final static int PERMISSIONS_REQUEST = 34;
    private final static int SELECT_PHOTO = 19;

    private EditText mNameEditText;
    private EditText mPriceEditText;
    private TextView mQuantityTextView;

    private String mProductName;
    private Uri mCurrentProductUri;
    private Button mOrderButton;

    private int mProductQuantity;
    private Button mIncreaseQuantityByOneButton;
    private Button mDecreaseQuantityByOneButton;

    private ImageView mProductImageView;
    private Button mSelectImageButton;
    private Bitmap mProductBitmap;

    private boolean mProductHasChanged = false;

    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.addProduct));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editProduct));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        if (mCurrentProductUri != null) {
            mOrderButton = (Button) findViewById(R.id.buttonOrder);
            mOrderButton.setVisibility(View.VISIBLE);
            mOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(Uri.parse("mailto:"));
                    intent.setType("text/plain");

                    intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.supplierEmail));
                    intent.putExtra(Intent.EXTRA_SUBJECT, mProductName);
                    startActivity(Intent.createChooser(intent, "Send Mail..."));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
        }
        mNameEditText = (EditText) findViewById(R.id.getName);
        mPriceEditText = (EditText) findViewById(R.id.getPrice);
        mQuantityTextView = (TextView) findViewById(R.id.finalQuantity);
        mIncreaseQuantityByOneButton = (Button) findViewById(R.id.buttonIncrease);
        mIncreaseQuantityByOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProductQuantity++;
                mQuantityTextView.setText(String.valueOf(mProductQuantity));
            }
        });

        mDecreaseQuantityByOneButton = (Button) findViewById(R.id.buttonDecrease);
        mDecreaseQuantityByOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mProductQuantity > 0) {
                    mProductQuantity--;
                    mQuantityTextView.setText(String.valueOf(mProductQuantity));
                } else {
                    Toast.makeText(EditorActivity.this, getString(R.string.toast_invalidQuantity), Toast.LENGTH_SHORT).show();
                }
            }
        });
        mProductImageView = (ImageView) findViewById(R.id.image2);
        mSelectImageButton = (Button) findViewById(R.id.buttonAddImage);
        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openImageSelector();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                        }
                        return;
                    }
                    Intent getIntent = new Intent(Intent.ACTION_PICK);
                    getIntent.setType("image/*");
                    startActivityForResult(getIntent, SELECT_PHOTO);
                }
            }
        });


        setOnTouchListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null) {

            Uri selectedImage = data.getData();
            Log.v("EditorActivity", "Uri: " + selectedImage.toString());
            String[] filePatchColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePatchColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePatchColumn[0]);

            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            mProductBitmap = BitmapFactory.decodeFile(picturePath);
            mProductBitmap = getBitmapFromUri(selectedImage);
            mProductImageView = (ImageView) findViewById(R.id.image2);
            mProductImageView.setImageBitmap(mProductBitmap);
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        mProductImageView = (ImageView) findViewById(R.id.image2);
        int targetW = mProductImageView.getWidth();
        int targetH = mProductImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoWidth = bmOptions.outWidth;
            int photoHeight = bmOptions.outHeight;

            int scaleFactor = Math.min(photoWidth / targetW, photoHeight / targetH);

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e("AddActivity", "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e("AddActivity", "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    private void saveProduct() {
        boolean nameIsEmpty = checkFieldEmpty(mNameEditText.getText().toString().trim());
        boolean priceIsEmpty = checkFieldEmpty(mPriceEditText.getText().toString().trim());

        if (nameIsEmpty) {
            Toast.makeText(this, getString(R.string.toast_invalidName), Toast.LENGTH_SHORT).show();
        } else if (mProductQuantity <= 0) {
            Toast.makeText(this, getString(R.string.toast_invalidQuantityVal), Toast.LENGTH_SHORT).show();
        } else if (priceIsEmpty) {
            Toast.makeText(this, getString(R.string.toast_invalidPrice), Toast.LENGTH_SHORT).show();
        } else if (mProductBitmap == null) {
            Toast.makeText(this, getString(R.string.toast_invalidImage), Toast.LENGTH_SHORT).show();
        } else {
            String name = mNameEditText.getText().toString().trim();

            double price = Double.parseDouble(mPriceEditText.getText().toString().trim());

            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_NAME, name);
            values.put(ProductEntry.COLUMN_QUANTITY, mProductQuantity);
            values.put(ProductEntry.COLUMN_PRICE, price);

            if (mProductBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                boolean a = mProductBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                values.put(ProductEntry.COLUMN_IMAGE, byteArray);
            }

            if (mCurrentProductUri == null) {

                Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                if (newUri == null) {
                    Toast.makeText(this, getString(R.string.toast_insertFail),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_insertSuccess),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

                if (rowsAffected == 0) {
                    Toast.makeText(this, getString(R.string.toast_updateFail), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_updateSuccessful),
                            Toast.LENGTH_SHORT).show();
                }

            }
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mProductHasChanged) {
                    saveProduct();
                } else {
                    Toast.makeText(this, getString(R.string.toast_NoChange), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                } else {
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                                }
                            };
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_NAME,
                ProductEntry.COLUMN_PRICE,
                ProductEntry.COLUMN_QUANTITY,
                ProductEntry.COLUMN_IMAGE
        };

        switch (id) {
            case EXISTING_PRODUCT_LOADER:
                return new CursorLoader(
                        this,
                        mCurrentProductUri,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        if (data.moveToFirst()) {
            mProductName = data.getString(data.getColumnIndex(ProductEntry.COLUMN_NAME));
            mNameEditText = (EditText) findViewById(R.id.getName);
            mNameEditText.setText(mProductName);

            mPriceEditText = (EditText) findViewById(R.id.getPrice);
            mPriceEditText.setText(data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRICE)));

            mQuantityTextView = (TextView) findViewById(R.id.finalQuantity);
            mProductQuantity = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_QUANTITY));
            mQuantityTextView.setText(String.valueOf(mProductQuantity));

            byte[] bytesArray = data.getBlob(data.getColumnIndexOrThrow(ProductEntry.COLUMN_IMAGE));
            if (bytesArray != null) {
                mProductBitmap = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.length);
                mProductImageView = (ImageView) findViewById(R.id.image2);
                mProductImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO);
    }

    private boolean checkFieldEmpty(String string) {
        return TextUtils.isEmpty(string) || string.equals(".");
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.promptProductDelete));
        builder.setPositiveButton(getString(R.string.promptDelete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.promptCancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Dismiss the dialog and continue editing the product
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.delete_toast_deleteFail), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_deleteSuccessful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.promptLeave));
        builder.setPositiveButton(getString(R.string.promptYes), discardButtonClickListener);
        builder.setNegativeButton(getString(R.string.promptCancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setOnTouchListener() {
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mIncreaseQuantityByOneButton.setOnTouchListener(mTouchListener);
        mDecreaseQuantityByOneButton.setOnTouchListener(mTouchListener);
        mSelectImageButton.setOnTouchListener(mTouchListener);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.getText().clear();
        mQuantityTextView.setText("");
    }
}
