package com.example.oquizadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.solver.widgets.Snapshot;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QuestionsActivity extends AppCompatActivity {

    private Button add,excel;
    private RecyclerView recyclerView;
    private QuestionsAdapter adapter;
    public static List<QuestionModel> list;
    private Dialog loadingDialog;
    private DatabaseReference myRef;
    private String categoryname;
    private TextView loadingText;
    private String setId;
    public static final int CELL_COUNT = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolbar);
        myRef = FirebaseDatabase.getInstance().getReference();

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        loadingText = loadingDialog.findViewById(R.id.textViewloading);

        setSupportActionBar(toolbar);

         categoryname = getIntent().getStringExtra("category");
        setId = getIntent().getStringExtra("setId");
        getSupportActionBar().setTitle(categoryname);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        add = findViewById(R.id.add_btn);
        excel = findViewById(R.id.excel_btn);
        recyclerView = findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();
        adapter = new QuestionsAdapter(list, categoryname, new QuestionsAdapter.DeleteListner() {
            @Override
            public void onLongClick(final int position, final String id) {
                new AlertDialog.Builder(QuestionsActivity.this,R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Question")
                        .setMessage("Are you sure . you want to delete this Question ?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadingDialog.show();
                                myRef.child("SETS").child(setId).child(id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                         list.remove(position);
                                         adapter.notifyItemRemoved(position);

                                        }else {
                                            Toast.makeText(QuestionsActivity.this, "Failed To Delete", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        getData(categoryname,setId);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addquestion = new Intent(QuestionsActivity.this,AddQuestionActivity.class);
                addquestion.putExtra("categoryname",categoryname);
                addquestion.putExtra("setId",setId);
                startActivity(addquestion);
            }
        });

        excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //storage read permission check
                if (ActivityCompat.checkSelfPermission(QuestionsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    selectFile();
                }else {
                    ActivityCompat.requestPermissions(QuestionsActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},101);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectFile();
            }else {
                Toast.makeText(this, "Please Allow Access", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void selectFile(){

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");//all file type
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent,"select file"),102);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102){
            if (resultCode == RESULT_OK){

                String filePath = data.getData().getPath();
                if (filePath.endsWith(".xlsx")){
                    readFile(data.getData());
                }else{
                    Toast.makeText(this, "please select an excel file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
    private void getData(String categoryname, final String setId){
        loadingDialog.show();
        myRef
                .child("SETS").child(setId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot1 :snapshot.getChildren()){
                    String id = snapshot1.getKey();
                    String question = snapshot1.child("question").getValue().toString();
                    String a = snapshot1.child("optionA").getValue().toString();
                    String b = snapshot1.child("optionB").getValue().toString();
                    String c = snapshot1.child("optionC").getValue().toString();
                    String d = snapshot1.child("optionD").getValue().toString();
                    String correctANS = snapshot1.child("correctANS").getValue().toString();

                    list.add(new QuestionModel(id,question,a,b,c,d,correctANS,setId));
                }
                loadingDialog.dismiss();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });

    }
    ///for excel file reading///
    private void readFile(final Uri fileUri){
        loadingText.setText("Loading Questions...");
        loadingDialog.show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final HashMap<String , Object> parentMap = new HashMap<>();
                final List<QuestionModel> tempList = new ArrayList<>();

                try {////try cache is used because may exception occur///
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                    XSSFSheet sheet = workbook.getSheetAt(0);
                    FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                    int rowsCount = sheet.getPhysicalNumberOfRows();
                    if (rowsCount > 0){

                        for (int r = 0; r < rowsCount; r++){

                            Row row = sheet.getRow(r);
                            if (row.getPhysicalNumberOfCells() == CELL_COUNT) {

                                String question = getCellData(row,0,formulaEvaluator);
                                String a = getCellData(row,1,formulaEvaluator);
                                String b = getCellData(row,2,formulaEvaluator);
                                String c = getCellData(row,3,formulaEvaluator);
                                String d  = getCellData(row,4,formulaEvaluator);
                                String correctAns = getCellData(row,5,formulaEvaluator);

                                if (correctAns.equals(a) || correctAns.equals(b) || correctAns.equals(c) || correctAns.equals(d)){

                                    HashMap<String, Object> questionMap = new HashMap<>();
                                    questionMap.put("question",question);
                                    questionMap.put("optionA" ,a);
                                    questionMap.put("optionB" ,b);
                                    questionMap.put("optionC" ,c);
                                    questionMap.put("optionD" ,d);
                                    questionMap.put("correctANS" ,correctAns);
                                    questionMap.put("setId" ,setId);

                                    String id = UUID.randomUUID().toString();

                                    parentMap.put(id , questionMap);

                                    tempList.add(new QuestionModel(id, question,a , b , c , d ,correctAns,setId));

                                }else {
                                    final int finalR1 = r;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadingText.setText("Loading...");
                                            loadingDialog.dismiss();
                                            Toast.makeText(QuestionsActivity.this, "Row No.  "+(finalR1 +1)+" has no correct option  ", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }

                            }else {
                                final int finalR = r;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingText.setText("Loading...");
                                        loadingDialog.dismiss();
                                        Toast.makeText(QuestionsActivity.this, "Row no. "+(finalR +1)+" has invalid data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return;
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("uploading...");

                                FirebaseDatabase.getInstance().getReference()
                                        .child("SETS").child(setId).updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            list.addAll(tempList);
                                            adapter.notifyDataSetChanged();
                                        }else {
                                            loadingText.setText("Loading...");
                                            Toast.makeText(QuestionsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });

                            }
                        });


                    }else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Loading...");
                                loadingDialog.dismiss();
                                Toast.makeText(QuestionsActivity.this, "file is empty", Toast.LENGTH_SHORT).show();
                            }
                        });

                        return;
                    }

                } catch (final FileNotFoundException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingText.setText("Loading...");
                            loadingDialog.dismiss();
                            Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (final IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingText.setText("Loading...");
                            loadingDialog.dismiss();
                            Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.notifyDataSetChanged();
    }

    private String getCellData(Row row,int cellPosition,FormulaEvaluator formulaEvaluator){

        String value = "";

        Cell cell = row.getCell(cellPosition);

        switch (cell.getCellType()){

            case  Cell.CELL_TYPE_BOOLEAN:
                return value + cell.getBooleanCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return value + cell.getNumericCellValue();

             case Cell.CELL_TYPE_STRING:
                 return value + cell.getStringCellValue();

            default:
                return value;

        }

    }
}