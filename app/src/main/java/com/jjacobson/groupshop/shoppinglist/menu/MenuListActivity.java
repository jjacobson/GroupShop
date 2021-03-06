package com.jjacobson.groupshop.shoppinglist.menu;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appinvite.FirebaseAppInvite;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.jjacobson.groupshop.BaseActivity;
import com.jjacobson.groupshop.R;
import com.jjacobson.groupshop.profile.Profile;
import com.jjacobson.groupshop.profile.SignInActivity;
import com.jjacobson.groupshop.shoppinglist.list.List;
import com.jjacobson.groupshop.shoppinglist.list.ShoppingListActivity;
import com.jjacobson.groupshop.util.ShareUtil;

import java.util.HashMap;

public class MenuListActivity extends BaseActivity {

    private DatabaseReference userListsRef;
    private DatabaseReference userRef;
    private DatabaseReference listsRef;

    private MenuListAdapter adapter;

    private ValueEventListener userListener;
    private Query userQuery;
    private Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_list);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getResources().getString(R.string.title_activity_menu_list));

        // database
        this.userListsRef = database.getReference()
                .child("users")
                .child(getUid())
                .child("lists");

        // user
        this.userRef = database.getReference()
                .child("users")
                .child(getUid());

        // database
        this.listsRef = database.getReference()
                .child("lists");

        // profile query
        this.userQuery = database.getReference()
                .child("user_profiles")
                .child(getUid());
        userListener = userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                profile = dataSnapshot.getValue(Profile.class);
                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        checkForInvitation();

        //ui
        initDrawer();
        initFab();
        initRecycler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.cleanup();
        userQuery.removeEventListener(userListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ShareUtil.INVITATION) {
            ShareUtil.shareComplete(this, resultCode, data);
        }
    }

    private void checkForInvitation() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData data) {
                        if (data == null) {
                            return;
                        }
                        // Get the deep link
                        Uri link = data.getLink();
                        FirebaseAppInvite invite = FirebaseAppInvite.getInvitation(data);
                        if (invite != null) {
                            String id = invite.getInvitationId();
                        }
                        String listId = link.getQueryParameter("list_id");
                        String inviteId = link.getQueryParameter("invite_id");
                        onInvitationReceived(inviteId, listId);

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    /**
     * Called after an invitation is successfully received and parameters parsed
     *
     * @param invite id
     * @param list   id
     */
    private void onInvitationReceived(final String invite, final String list) {
        System.out.println("called!!! inv is " + invite + " list is " + list);
        userRef.child("invite").setValue(invite).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                onInvitationProcessed(list);
            }
        });
    }

    private void onInvitationProcessed(final String list) {
        listsRef.child(list)
                .child("users")
                .child(getUid())
                .setValue(true)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        userRef.child("lists")
                                .child(list)
                                .setValue(true);
                    }
                });
    }


    /**
     * Initialize the actionbar drawer
     */
    private void initDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView drawerOptions = (NavigationView) findViewById(R.id.drawer_nav_view);
        drawerOptions.setNavigationItemSelectedListener(new DrawerItemListener(this));
    }

    /**
     * Initialize the new list floating action button
     */
    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_list);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayCreateDialog();
            }
        });
    }

    /**
     * Initialize the recyclerview
     */
    private void initRecycler() {
        RecyclerView lists = (RecyclerView) findViewById(R.id.menu_list_recycler);
        Query listsQuery = userListsRef.orderByValue().equalTo(true);
        MenuListAdapter adapter = new MenuListAdapter(this, Object.class, R.layout.row_shopping_list, MenuListHolder.class, listsQuery);
        DividerItemDecoration divider = new DividerItemDecoration(lists.getContext(), DividerItemDecoration.VERTICAL);
        lists.addItemDecoration(divider);
        this.adapter = adapter;
        lists.setLayoutManager(new LinearLayoutManager(this));
        lists.setAdapter(adapter);
    }

    private void updateUI() {
        NavigationView nav = (NavigationView) findViewById(R.id.drawer_nav_view);
        View header = nav.getHeaderView(0);
        if (getUser() == null || getUser().isAnonymous()) {
            header.findViewById(R.id.button_sign_in).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MenuListActivity.this, SignInActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            if (profile == null) {
                return;
            }
            header.findViewById(R.id.drawer_button_view).setVisibility(View.GONE);
            LinearLayout profileLayout = (LinearLayout) header.findViewById(R.id.drawer_profile_view);
            profileLayout.setVisibility(View.VISIBLE);

            // populate profile properties
            final ImageView imageView = (ImageView) findViewById(R.id.drawer_profile_image);
            final TextView nameText = (TextView) findViewById(R.id.drawer_profile_name);
            final TextView emailText = (TextView) findViewById(R.id.drawer_profile_email);
            nameText.setText(profile.getName());
            emailText.setText(profile.getEmail());
            RequestOptions options = new RequestOptions().centerCrop();
            if (profile.getPhotoUri() != null) {
                Glide.with(this).asBitmap().apply(options).load(profile.getPhotoUri()).into(new BitmapImageViewTarget(imageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        imageView.setImageDrawable(circularBitmapDrawable);
                    }
                });
            } else {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_grey));
            }
        }
    }

    /**
     * Display the list name dialog prompt
     */
    private void displayCreateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_list_name, null);
        builder.setView(dialogView);

        final EditText text = (EditText) dialogView.findViewById(R.id.dialog_list_name);

        builder.setTitle(getResources().getString(R.string.new_list_title_text));
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = text.getText().toString();
                if (name.equals("")) {
                    return;
                }
                List list = createNewList(name);
                saveList(list);
                Intent intent = new Intent(MenuListActivity.this, ShoppingListActivity.class);
                intent.putExtra("list_extra", list);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Display the list name dialog prompt
     */
    private void displayInviteReceivedDialog(List list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.invitation_received_title));
        builder.setMessage(getResources().getString(R.string.invitation_received_message));
        builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {


            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Create a new list
     *
     * @param name of list
     * @return the created list
     */
    private List createNewList(String name) {
        List list = new List();
        list.setName(name);

        HashMap<String, Boolean> userMap = new HashMap<>();
        userMap.put(getUid(), true);
        list.setUsers(userMap);

        String key = userListsRef.push().getKey();
        userListsRef.child(key).setValue(true);
        list.setKey(key);
        return list;
    }

    /**
     * Save a list to the database
     *
     * @param list to save
     */
    public void saveList(List list) {
        listsRef.child(list.getKey()).setValue(list);
    }

    /**
     * Delete the list
     */
    public void deleteList(List list) {
        userListsRef.child(list.getKey()).setValue(false);
    }
}
