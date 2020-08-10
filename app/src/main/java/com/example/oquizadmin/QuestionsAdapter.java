package com.example.oquizadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.Viewholder> {

    private List<QuestionModel>  list;
    private String category;
    private DeleteListner listner;

    public QuestionsAdapter(List<QuestionModel> list,String category,DeleteListner listner) {
        this.list = list;
        this.category = category;
        this.listner = listner;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_item,parent,false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        String question = list.get(position).getQuestion();
        String answer = list.get(position).getAnswer();
        holder.setData(question,answer,position);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class Viewholder extends RecyclerView.ViewHolder{

        private TextView question,answer;

        public Viewholder(@NonNull View itemView) {
            super(itemView);

            question = itemView.findViewById(R.id.question);
            answer = itemView.findViewById(R.id.answer);
        }

        private void setData(String question, String answer, final int position){
            this.question.setText(position+1+". "+question);
            this.answer.setText("Ans. "+answer);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent editIntent = new Intent(itemView.getContext(),AddQuestionActivity.class);
                    editIntent.putExtra("categoryname",category);
                    editIntent.putExtra("setId",list.get(position).getSet());
                    editIntent.putExtra("position",position);
                    itemView.getContext().startActivity(editIntent);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                   listner.onLongClick(position,list.get(position).getId());
                    return false;
                }
            });
        }

    }

    public interface DeleteListner{
        void onLongClick(int position,String id);
    }

}
