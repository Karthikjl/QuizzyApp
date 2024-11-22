package com.jk.Quizzy;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
public class MainActivity extends AppCompatActivity {

    private TextView questionTextView, scoreTextView, quizCountTextView, feedbackTextView, correctnessTextView;
    private Button optionAButton, optionBButton, optionCButton, optionDButton, nextButton;

    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int quizRound = 1;
    private boolean isAnswerSelected = false; // To check if an answer is selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionTextView = findViewById(R.id.questionTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        quizCountTextView = findViewById(R.id.quizCountTextView);
        feedbackTextView = findViewById(R.id.feedbackTextView);
        correctnessTextView = findViewById(R.id.correctnessTextView); // New TextView for correctness feedback
        optionAButton = findViewById(R.id.optionAButton);
        optionBButton = findViewById(R.id.optionBButton);
        optionCButton = findViewById(R.id.optionCButton);
        optionDButton = findViewById(R.id.optionDButton);
        nextButton = findViewById(R.id.nextButton);

        fetchQuestions();

        View.OnClickListener answerClickListener = v -> {
            Button clickedButton = (Button) v;
            if (!isAnswerSelected) {
                checkAnswer(clickedButton.getText().toString());
            }
        };

        optionAButton.setOnClickListener(answerClickListener);
        optionBButton.setOnClickListener(answerClickListener);
        optionCButton.setOnClickListener(answerClickListener);
        optionDButton.setOnClickListener(answerClickListener);

        nextButton.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size()) {
                showNextQuestion();
            } else {
                Toast.makeText(MainActivity.this, "Quiz Completed! Your Score: " + score, Toast.LENGTH_LONG).show();
                quizRound++;
                fetchQuestions(); // Fetch next set of questions
            }
        });
    }

    private void fetchQuestions() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuizApi api = retrofit.create(QuizApi.class);
        // Fetching 10 multiple-choice questions from the General Knowledge category (Category 9)
        api.getQuestions(10, "multiple", 9).enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, retrofit2.Response<QuizResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body().results;
                    currentQuestionIndex = 0; // Reset question index for the new set
                    showNextQuestion();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNextQuestion() {
        isAnswerSelected = false;
        feedbackTextView.setText(""); // Clear feedback text
        correctnessTextView.setVisibility(View.GONE); // Hide correctness message initially

        if (currentQuestionIndex < questionList.size()) {
            Question question = questionList.get(currentQuestionIndex);

            // Decode HTML entities in the question
            String decodedQuestion = Html.fromHtml(question.question, Html.FROM_HTML_MODE_LEGACY).toString();
            questionTextView.setText(decodedQuestion);

            List<String> options = new ArrayList<>(question.incorrectAnswers);
            options.add(question.correctAnswer);
            Collections.shuffle(options);

            optionAButton.setText(options.get(0));
            optionBButton.setText(options.get(1));
            optionCButton.setText(options.get(2));
            optionDButton.setText(options.get(3));

            quizCountTextView.setText("Quiz Round: " + quizRound + " | Question: " + (currentQuestionIndex + 1) + "/10");

            nextButton.setVisibility(View.INVISIBLE); // Hide Next button initially
        }
    }

    private void checkAnswer(String selectedAnswer) {
        Question question = questionList.get(currentQuestionIndex);
        isAnswerSelected = true;

        if (selectedAnswer.equals(question.correctAnswer)) {
            score++;
            feedbackTextView.setText("Correct!");
            correctnessTextView.setText("Your answer is correct.");
            correctnessTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            feedbackTextView.setText("Incorrect!");
            correctnessTextView.setText("The correct answer is: " + question.correctAnswer);
            correctnessTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        scoreTextView.setText(" | Score: " + score);
        correctnessTextView.setVisibility(View.VISIBLE); // Show the correctness message
        nextButton.setVisibility(View.VISIBLE); // Show Next button after answering
        currentQuestionIndex++;
    }

    // Retrofit interface to define the API call
    interface QuizApi {
        @GET("api.php")
        Call<QuizResponse> getQuestions(
                @Query("amount") int amount,
                @Query("type") String type,
                @Query("category") int category // Added the category parameter
        );
    }

    // Response model for the quiz API response
    static class QuizResponse {
        @SerializedName("results")
        List<Question> results;
    }

    // Question model for each quiz question
    static class Question {
        String question;
        @SerializedName("correct_answer")
        String correctAnswer;
        @SerializedName("incorrect_answers")
        List<String> incorrectAnswers;
    }
}
