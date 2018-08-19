package net.dimk.simplefilemanager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    String[] spnAction={"Select Action", "New Folder", "Cut", "Copy", "Paste", "Rename", "Delete"};

    public ListView lst_Folder;
    public String dirPath="";
    public String ParentdirPath="";
    public ArrayList<String> theNamesOfFiles;
    public ArrayList<Integer> intImages;
    public TextView txtPath;
    public Spinner spin;
    public CustomList customList;
    public File dir;
    public ArrayList<Integer> intSelected;
    public ArrayList<String> strSelected;
    public Integer intCutORCopy=0; //0=Nothing, 1=Cut, 2=Copy

    //Get Permission to access EXTERNAL STORAGE
    public final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    public final int EXTERNAL_REQUEST = 138;

    public boolean requestForExternalStoragePermission() {

        boolean isPermissionOn = true;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            if (!canAccessExternalSd()) {
                isPermissionOn = false;
                requestPermissions(EXTERNAL_PERMS, EXTERNAL_REQUEST);
            }
        }

        return isPermissionOn;
    }

    public boolean canAccessExternalSd() {
        return (hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set Orientation to Portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        //Get Permission
        requestForExternalStoragePermission();

        //Initialize ArrayLists
        theNamesOfFiles = new ArrayList<String >();
        intImages = new ArrayList<Integer>();
        strSelected = new ArrayList<String>();
        intSelected = new ArrayList<Integer>();

        //Get ListView
        lst_Folder=(ListView)findViewById(R.id.lstFolder);

        //Get txtvPath
        txtPath=(TextView) findViewById(R.id.txtvPath);

        //Set dirPath to EXTERNAL STORAGE
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            dirPath = String.valueOf(android.os.Environment.getExternalStorageDirectory());
        }

        ///mounted
        RefreshListView();
        set_Adapter();
        setPath();

        //Getting the instance of Spinner and applying OnItemSelectedListener on it
        spin = (Spinner) findViewById(R.id.spinner);
        spin.setOnItemSelectedListener(this);

        //Creating the ArrayAdapter instance having the action name list
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item,spnAction);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin.setAdapter(aa);

        //OnItem Click Listview

        //set strict mode to overide errors for startActivity
        //https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        lst_Folder.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try{
                    ParentdirPath = dirPath+"/..";
                    dirPath = dirPath+"/"+theNamesOfFiles.get(position);

                    File f = new File(dirPath);
                    if (f.isDirectory()){
                        RefreshListView();
                        RefreshAdapter();
                        setPath();
                    }else{
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        File file = new File(dirPath);

                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                        String type = mime.getMimeTypeFromExtension(ext);
                        intent.setDataAndType(Uri.fromFile(file), type);
                        startActivity(intent);
                    }

                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });

        //Select folders and files
        lst_Folder.setChoiceMode(lst_Folder.CHOICE_MODE_MULTIPLE);


        //On Long Click select items
        lst_Folder.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                //if click selected then deselect it
                if(intSelected.contains(i)){

                    intSelected.remove(intSelected.indexOf(i));
                    strSelected.remove(strSelected.indexOf(dirPath+"/"+theNamesOfFiles.get(i)));

                    lst_Folder.getChildAt(i).setBackgroundColor(Color.WHITE);

                }else {
                //Select items
                    strSelected.add(dirPath+"/"+theNamesOfFiles.get(i));
                    intSelected.add(i);

                    lst_Folder.getChildAt(i).setBackgroundColor(Color.BLUE);
                }

                return true;
            }
        });

    }

    public void ClearSelected(){
        strSelected.clear();
        intSelected.clear();
    }

    public void onParentDir_Click(View view){
        if (dirPath!="" && dirPath!="/"){
            String[] folders = dirPath.split("\\/");
            String[] folders2={};
            folders2 = Arrays.copyOf(folders, folders.length-1);
            dirPath = TextUtils.join("/", folders2);
        }

        if (dirPath==""){
            dirPath="/";
        }
        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void setPath(){
        txtPath.setText(dirPath);
    }

    public void RefreshListView() {
    try{

        dir = new File(dirPath);
        File[] filelist = dir.listFiles();

        //reset ArrayLists
        theNamesOfFiles.clear();
        intImages.clear();

        for (int i = 0; i < filelist.length; i++) {

            theNamesOfFiles.add(filelist[i].getName());
         //   intImages[i] = R.drawable.folder;

            if(filelist[i].isDirectory()==true){
                intImages.add(R.drawable.folder);
            }else if(filelist[i].isFile()==true){
                intImages.add(R.drawable.file);
            }else{
                intImages.add(R.drawable.file);
            }
        }
    }catch (Exception e){
//        String error = e.toString() + "\n\nMessage: " + e.getMessage();
//        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
    }

    }

    public void set_Adapter() {
        customList = new CustomList();
        lst_Folder.setAdapter(customList);
    }

    public void RefreshAdapter(){
        customList.notifyDataSetChanged();
    }

    //Performing action onItemSelected and onNothing selected for Spinner
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {


        if (spnAction[position]=="Cut"){
            //Cut
            Cut();
        }
        if (spnAction[position]=="Copy"){
            //Copy
            Copy();
        }
        if (spnAction[position]=="Paste"){
            //Paste
            Paste();
        }
        if (spnAction[position]=="New Folder"){
            //New Folder
            NewFolder();
        }
        if (spnAction[position]=="Delete"){
            //Delete
            Delete();
        }
        if (spnAction[position]=="Rename"){
            //Rename
            Rename();
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    // TODO Auto-generated method stub

    }

//  intCutORCopy=0; //0=Nothing, 1=Cut, 2=Copy REFERENCE

    public void Cut(){
        intCutORCopy=1;
        spin.setSelection(0);
    }

    public void Copy(){
        intCutORCopy=2;
        spin.setSelection(0);
    }

    public void Paste(){
        if (intSelected.size()>0){
            //Cut
            if (intCutORCopy==1){
                List<String> command = new ArrayList<String>();
                try {
                    for (int i=0; i<intSelected.size(); i++){
                    command.clear();
                    command.add("/system/bin/mv");
                    command.add(strSelected.get(i));
                    command.add(dirPath);

                        // start the subprocess
                        ProcessBuilder pb = new ProcessBuilder(command);
                        Process process = pb.start();
                        process.waitFor();
                        //Refresh ListView
                        RefreshListView();
                        RefreshAdapter();
                        setPath();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
                ResetValues();
            }
            //Copy
            if(intCutORCopy==2){
                List<String> command = new ArrayList<String>();
                try {
                    for (int i=0; i<intSelected.size(); i++){
                        command.clear();
                        command.add("/system/bin/cp");
                        command.add("-rf");
                        command.add(strSelected.get(i));
                        command.add(dirPath);

                        // start the subprocess
                        ProcessBuilder pb = new ProcessBuilder(command);
                        Process process = pb.start();
                        process.waitFor();
                        //Refresh ListView
                        RefreshListView();
                        RefreshAdapter();
                        setPath();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
                ResetValues();
            }
        }

    }

    public void NewFolder(){
        try{

            //NewFolder Dialog Builder
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.newfolder, null);
            dialogBuilder.setView(dialogView);

            final EditText txtNewFolder = (EditText) dialogView.findViewById(R.id.newfolder);

            dialogBuilder.setTitle("New Folder");
            dialogBuilder.setMessage("Enter name of new folder");
            dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String sNewFolderName = txtNewFolder.getText().toString();
                    File fNewFolder = new File(dirPath+"/"+sNewFolderName);
                    // create
                    Boolean bIsNewFolderCreated = fNewFolder.mkdir();
                    //Refresh ListView
                    RefreshListView();
                    RefreshAdapter();
                    setPath();
                }
            });
            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //pass
                }
            });
            AlertDialog b = dialogBuilder.create();
            b.show();


        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        spin.setSelection(0);
    }

    public void Delete(){
        List<String> command = new ArrayList<String>();
        try {
            for (int i=0; i<intSelected.size(); i++){
                command.clear();
                command.add("/system/bin/rm");
                command.add("-rf");
                command.add(strSelected.get(i).toString());

                // start the subprocess
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();
                process.waitFor();
                //Refresh ListView
                RefreshListView();
                RefreshAdapter();
                setPath();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        ResetValues();
    }

    public void Rename(){
        try{
            if(strSelected.size()==1){
                //RenameFolder Dialog Builder
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.newfolder, null);
                dialogBuilder.setView(dialogView);

                final EditText txtNewFolder = (EditText) dialogView.findViewById(R.id.newfolder);

                dialogBuilder.setTitle("Rename Folder");
                dialogBuilder.setMessage("Enter name of new folder");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        File f = new File(strSelected.get(0).toString());
                        File fRename = new File(dirPath+"/"+txtNewFolder.getText().toString());
                        f.renameTo(fRename);
                        //Refresh ListView
                        RefreshListView();
                        RefreshAdapter();
                        setPath();
                        //ResetValues
                        ResetValues();
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //pass
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();

            }
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        spin.setSelection(0);
    }

    public void ResetValues(){
        //Reset values
        spin.setSelection(0);
        intCutORCopy=0;
        intSelected.clear();
        strSelected.clear();
    }

    public void onMainStorage_Click(View view){
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            ///mounted
            dirPath = String.valueOf(android.os.Environment.getRootDirectory());

            RefreshListView();
            RefreshAdapter();
            setPath();
        }
    }

    public  void onSDCARD_Click(View view){
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            ///mounted
            dirPath = String.valueOf(android.os.Environment.getExternalStorageDirectory());

            RefreshListView();
            RefreshAdapter();
            setPath();
        }
    }

    public void onDownloads_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onImages_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onAudio_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onVideo_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onDCIM_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onDocuments_Click(View view){
        dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public void onApps_Click(View view){
        dirPath = "/data/app/";

        RefreshListView();
        RefreshAdapter();
        setPath();
    }

    public class CustomList extends BaseAdapter {

        @Override
        public int getCount() {
            return intImages.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            View view1 = getLayoutInflater().inflate(R.layout.custom_list, null);

            ImageView imageView = (ImageView) view1.findViewById(R.id.ItemIcon);
            TextView txtPath = (TextView) view1.findViewById(R.id.ItemName);

            imageView.setImageResource(intImages.get(i));
            txtPath.setText(theNamesOfFiles.get(i));

            return view1;
        }
    }
}
