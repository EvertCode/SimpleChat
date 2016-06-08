package com.evertcode.simplechat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Hebert on 07/06/2016.
 */
public class FirebaseHelper {

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    private final static String SEPARATOR = "__";
    private final static String USERS_PATH = "users";
    private final static String CONTACTS_PATH = "contacts";
    private final static String CHATS_PATH = "chats";

    private static class SingletonHolder {
        private static final FirebaseHelper INSTANCE = new FirebaseHelper();
    }

    public static FirebaseHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public FirebaseHelper() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public DatabaseReference getDatabaseReference() {
        return this.databaseReference;
    }

    public String getAuthUserEmail() {
        if (null != this.firebaseAuth) {
            return this.firebaseAuth.getCurrentUser().getEmail();
        }
        return null;
    }

    public DatabaseReference getUserReference(final String email) {
        DatabaseReference user = null;

        if (null != email) {
            final String emailKey = email.replace(".", "_");
            user = this.databaseReference.getRoot().child(USERS_PATH).child(emailKey);
        }

        return user;
    }

    public DatabaseReference getMyUserReference() {
        return this.getUserReference(this.getAuthUserEmail());
    }

    public DatabaseReference getContactsReference(final String email) {
        return this.getUserReference(email).child(CONTACTS_PATH);
    }

    public DatabaseReference getContactsReference() {
        return this.getContactsReference(this.getAuthUserEmail());
    }

    public DatabaseReference getOneContactReference(final String mainEmail, final String childEmail) {
        final String childKey = childEmail.replace(".", "_");
        return this.getUserReference(mainEmail).child(CONTACTS_PATH).child(childKey);
    }

    public DatabaseReference getChatsReference(final String receiver) {
        final String keySender = this.getAuthUserEmail().replace(".", "_");
        final String keyReceiver = receiver.replace(".", "_");

        String keyChat = keySender + SEPARATOR + keyReceiver;

        if (keySender.compareTo(keyReceiver) > 0) {
            keyChat = keyReceiver + SEPARATOR + keySender;
        }

        return this.databaseReference.getRoot().child(CHATS_PATH).child(keyChat);
    }

    public void changeUserConnectionStatus(final boolean online) {
        if (null != this.getMyUserReference()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("online", online);
            this.getMyUserReference().updateChildren(updates);
            notifyContactsOfConnectionChange(online);
        }
    }

    public void notifyContactsOfConnectionChange(final boolean online) {
        this.notifyContactsOfConnectionChange(online, false);
    }

    public void singOff() {
        this.notifyContactsOfConnectionChange(false, true);
    }

    private void notifyContactsOfConnectionChange(final boolean online, final boolean signoff) {
        final String userEmail = this.getAuthUserEmail();

        this.getContactsReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String email = child.getKey();
                    DatabaseReference databaseReference = getOneContactReference(email, userEmail);
                    databaseReference.setValue(online);
                }

                if (signoff) {
                    firebaseAuth.signOut();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
