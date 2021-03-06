package com.jjacobson.groupshop.shoppinglist.item;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.jjacobson.groupshop.R;
import com.jjacobson.groupshop.shoppinglist.list.ShoppingListActivity;

import java.util.Arrays;

/**
 * Created by Jeremiah on 7/11/2017.
 */

public class ItemPropertyDialog {

    private ShoppingListActivity activity;
    private Item item;
    private View view;

    public ItemPropertyDialog(ShoppingListActivity activity) {
        this.activity = activity;
    }

    /**
     * Display new item creation dialog
     */
    public void displayCreateItemDialog() {
        this.item = new Item();
        AlertDialog.Builder builder = initItemDialog();
        builder.setTitle(activity.getResources().getString(R.string.new_item_title_text));
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText name = (EditText) view.findViewById(R.id.dialog_item_name);
                if (name.getText().toString().equals("")) {
                    return;
                }
                activity.createItem(item);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    /**
     * Display item edit dialog
     *
     * @param item to edit
     */
    public void displayEditItemDialog(final Item item) {
        this.item = item;
        AlertDialog.Builder builder = initItemDialog();
        builder.setTitle(item.getName());
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText name = (EditText) view.findViewById(R.id.dialog_item_name);
                if (name.getText().toString().equals("")) {
                    return;
                }
                activity.saveItem(item);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        updateUI();
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    /**
     * Display the list name dialog prompt
     */
    private AlertDialog.Builder initItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(activity);
        this.view = inflater.inflate(R.layout.dialog_edit_item, null);
        builder.setView(view);

        // ui
        initNameText();
        initSpinner();
        initCounter();

        return builder;
    }

    /**
     * Initialize name edit text
     */
    private void initNameText() {
        AutoCompleteTextView name = (AutoCompleteTextView) view.findViewById(R.id.dialog_item_name);
        name.addTextChangedListener(new ItemNameListener(this));
        name.setAdapter(new ArrayAdapter<>(activity.getApplicationContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(activity.getResources().getStringArray(R.array.products))));
    }

    /**
     * Initialize spinner with default hint
     */
    private void initSpinner() {
        Spinner spinner = (Spinner) view.findViewById(R.id.unit_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                R.array.units_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new UnitSpinnerListener(activity, this, spinner));

    }

    /**
     * Initialize the quantity counter
     */
    private void initCounter() {
        Button increase = (Button) view.findViewById(R.id.button_increase);
        Button decrease = (Button) view.findViewById(R.id.button_decrease);
        EditText quantity = (EditText) view.findViewById(R.id.number_picker_display);
        quantity.addTextChangedListener(new QuanitityTextListener(this));
        QuantityButtonListener listener = new QuantityButtonListener(view, this);
        increase.setOnClickListener(listener);
        decrease.setOnClickListener(listener);
    }

    /**
     * Update dialog UI with existing values
     */
    private void updateUI() {
        EditText name = (EditText) view.findViewById(R.id.dialog_item_name);
        EditText count = (EditText) view.findViewById(R.id.number_picker_display);
        Spinner spinner = (Spinner) view.findViewById(R.id.unit_spinner);
        // name
        name.setText(item.getName());
        // count
        count.setText(String.valueOf(item.getCount()));
        // units
        if (item.getUnit() != null && !item.getUnit().equals("")) {
            ArrayAdapter<String> items = (ArrayAdapter<String>) spinner.getAdapter();
            spinner.setSelection(items.getPosition(item.getUnit()));
        }
    }

    /**
     * Get the item from this dialog
     *
     * @return item
     */
    public Item getItem() {
        return item;
    }
}
