package com.esafirm.imagepicker.features;

import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.AccessToken.getCurrentAccessToken;

/**
 * Created by Luca on 16/01/2017.
 */
public class FacebookUtils {

//    public static boolean isLoggedIn() {
//        AccessToken a = AccessToken.getCurrentAccessToken();
//        return a!=null;
//    }

    public static void getFbAlbumPhotos(String albumId, final ListCallback cb) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "images");
        /* make the API call */
        new GraphRequest(
                getCurrentAccessToken(),
                "/" + albumId + "/photos",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
            /* handle the result */
                        List<String[]> list = new ArrayList<String[]>();
                        try {
                            if (response.getError() == null) {
                                JSONObject object = response.getJSONObject();
                                if(object!=null && object.has("data")) {
                                    JSONArray photos = object.getJSONArray("data");
                                    JSONObject imagesobj, photoobj;
                                    JSONArray images;
                                    String id, link;
                                    int width, maxW;
                                    for(int i=0; i<photos.length(); i++) {
                                        imagesobj = photos.getJSONObject(i);
                                        id = imagesobj.getString("id");
                                        link = null;
                                        maxW = 0;
                                        images = imagesobj.getJSONArray("images");
                                        for(int j=0; j<images.length(); j++) {
                                            photoobj = images.getJSONObject(j);
                                            width = photoobj.getInt("width");
                                            if(width>maxW) {
                                                maxW = width;
                                                link = photoobj.getString("source");
                                            }
                                        }
                                        if(link!=null)
                                            list.add(new String[]{link,id});
                                    }
                                }

                            } else {
//                                //TODO
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        cb.call(list);
                    }
                }
        ).executeAsync();
    }

    public static void getFbAlbums(final ListCallback cb) {
        GraphRequest request = GraphRequest.newMeRequest(
                getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        List<String[]> list = new ArrayList<String[]>();
                        try {
                            JSONObject albumsobject = object.getJSONObject("albums");
                            if(albumsobject!=null && albumsobject.has("data")) {

                                JSONArray albumsobjectdata = albumsobject.getJSONArray("data");

                                JSONObject albumobj;
                                String albumid, albumname;
                                for(int i=0; i<albumsobjectdata.length(); i++) {
                                    albumobj = albumsobjectdata.getJSONObject(i);
                                    if(albumobj.has("id")) {
                                        albumid = albumobj.getString("id");
                                        albumname = albumobj.optString("name", null);
                                        list.add(new String[]{albumid, albumname});
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        cb.call(list);
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "albums{id,name}");//"albums{id,cover_photo,name}");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public interface ListCallback {
        void call(List list);
    }

    public static class FinalCounter {

        private int val;

        public FinalCounter(int initialVal) {
            val = initialVal;
        }

        public void increment() {
            val++;
        }

        public void decrement() {
            val--;
        }

        public int getVal() {
            return val;
        }
    }
}
