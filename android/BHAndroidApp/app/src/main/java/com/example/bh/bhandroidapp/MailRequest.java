package com.example.bh.bhandroidapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by BH on 2017-07-11.
 */

public class MailRequest {
    private URL requestURL;
    private HttpURLConnection connection;
    private Context context = null;
    private RequestAsyncTask asyncTask;
    private Handler handler = new Handler();
    private ProgressDialog asyncDialog;
    private Resources res;
    public MailRequest(String url,Context c){
        try{
            requestURL = new URL(url);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        context = c;
        asyncDialog = new ProgressDialog(context);
        res = c.getResources();
    }

    public void requestToServer(){
        asyncTask = new RequestAsyncTask();
        asyncTask.execute();
    }

    protected class RequestAsyncTask extends AsyncTask<String,String,ArrayList<MailEntry>>{

        @Override
        protected void onPreExecute(){
            super.onPreExecute();

            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setTitle(res.getString(R.string.wait_please));
            asyncDialog.setMessage(res.getString(R.string.now_working_for_mail_list));
            asyncDialog.setCancelable(false);
            asyncDialog.show();
        }
        @Override
        protected void onPostExecute(final ArrayList<MailEntry> mailEntry) {
            super.onPostExecute(mailEntry);
            MailListViewAdapter adapter = ((ActivityMain)context).getMailListAdapter();
            if(mailEntry == null){
                adapter.clearItemAll();
                adapter.notifyDataSetChanged();
                return;
            }

            adapter.clearItemAll();
            for(int i = 0 ;i<mailEntry.size();i++){
                adapter.addItemToAll(mailEntry.get(i));
            }

            adapter.notifyDataSetChanged();
            asyncDialog.dismiss();
        }
        protected void dismissProgressDialog(){
            handler.post(new Runnable(){
                public void run(){
                    asyncDialog.dismiss();
                }
            });
        }
        @Override
        protected ArrayList<MailEntry> doInBackground(String... params) {
            ArrayList<MailEntry> retEntries = new ArrayList<MailEntry>();
            try{
                connection = (HttpURLConnection) requestURL.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
            }catch(Exception e){
                Log.e("requestToServer()","openConnection error");
                dismissProgressDialog();
                e.printStackTrace();
                return null;
            }

            try{
                if(connection == null){
                    handler.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,res.getString(R.string.server_is_not_working),Toast.LENGTH_SHORT).show();
                            dismissProgressDialog();
                        }
                    });
                    return null;
                }

                if(connection.getResponseCode() != 200){
                    connection.disconnect();

                    handler.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,res.getString(R.string.server_is_not_working),Toast.LENGTH_SHORT).show();
                            dismissProgressDialog();
                        }
                    });
                    return null;
                }
            }catch(Exception e){
                dismissProgressDialog();
                Log.e("requestToServer()","getResponseCode");
                e.printStackTrace();
                handler.post(new Runnable(){
                    public void run(){
                        Toast.makeText(context,res.getString(R.string.server_is_not_working),Toast.LENGTH_SHORT).show();
                        dismissProgressDialog();
                    }
                });
                return null;
            }
            try{

                InputStream bis = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(bis));
                StringBuffer sb = new StringBuffer();
                String inputLine = "";

                while((inputLine = br.readLine() ) != null){
                    sb.append(inputLine);
                }

                JSONArray jsonArray = new JSONArray(sb.toString());
                if(jsonArray.length() == 0){
                    handler.post(new Runnable(){
                        public void run(){
                            Toast.makeText(context,res.getString(R.string.there_is_no_mail), Toast.LENGTH_SHORT).show();

                            dismissProgressDialog();
                        }
                    });

                    return null;
                }
                for(int i = 0 ;i<jsonArray.length();i++){
                    JSONObject tempJsonObject = jsonArray.getJSONObject(i);
                    MailEntry tempMailEntry = new MailEntry(i,tempJsonObject.getString("title"),tempJsonObject.getString("sender"),
                            tempJsonObject.getString("mail_date"),tempJsonObject.getString("inner_text"));

                    StringBuffer tempRealReceiver = new StringBuffer("");
                    StringBuffer tempRefReceiver = new StringBuffer("");
                    for(int j = 0 ;j<tempJsonObject.getInt("real_size");j++){
                        tempRealReceiver.append(tempJsonObject.getJSONArray("real_receiver").getString(j));
                        if(j != tempJsonObject.getInt("real_size")-1){
                            tempRealReceiver.append(", ");
                        }

                    }
                    for(int j = 0; j<tempJsonObject.getInt("ref_size");j++){
                        tempRefReceiver.append(tempJsonObject.getJSONArray("ref_receiver").getString(j));
                        if(j != tempJsonObject.getInt("ref_size")-1){
                            tempRefReceiver.append(", ");
                        }
                    }
                    tempMailEntry.setRealReceiver(tempRealReceiver.toString());
                    if(tempJsonObject.getInt("ref_size") !=0){
                        tempMailEntry.setRefReceiver(tempRefReceiver.toString());
                        tempMailEntry.setExistRef(true);
                    }
                    retEntries.add(tempMailEntry);
                }

            }catch(Exception e){
                dismissProgressDialog();
                Log.e("requestToServer()","assign response, reader err");
                e.printStackTrace();
            }

            try{
                connection.disconnect();
            }catch(Exception e){
                dismissProgressDialog();
                Log.e("requestToServer()","print json result err");
                e.printStackTrace();
            }

            return retEntries;
        }
    }
}
