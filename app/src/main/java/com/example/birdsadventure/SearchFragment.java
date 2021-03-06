package com.example.birdsadventure;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class SearchFragment extends Fragment implements View.OnClickListener {
    //Database instance
    FirebaseFirestore db;
    /**
     *  Decleration of Edit Text boxes,Button
     */
    EditText txtSearchBird;
    Button btnSearch;
    /**
     * Spinner is Display  Location Selected and for Selection
     */
    Spinner spinnerLocation;
    TextView txt_no_search;

    ArrayList<Location> locationList;

    private ArrayList<Bird> birdsList;

    private NavController navController;
    /**
     * This Method Show The Search Birds
     */

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);

    }
    /**
     * This Method Display the view where user can Search Places with Search button
     * with Spinner user can Select the list of Location
     * OnClickListener is set for Search Button to Display All Birds
     * @param view
     * @param savedInstanceState
     */

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        txtSearchBird = getActivity().findViewById(R.id.text_search_bird);
        btnSearch = getActivity().findViewById(R.id.button_search);
        spinnerLocation = (Spinner) getActivity().findViewById(R.id.spinner_locations);
        txt_no_search = getActivity().findViewById(R.id.txt_no_search);
        txt_no_search.setText("");

        btnSearch.setOnClickListener(this);

        getAllLocations();

        getAllBirds("", "--All Locations--");
    }
    /**
     *This Method is to get the  Location Name with Spinner
     */
    private void getAllLocations() {

        locationList = new ArrayList<Location>();
        locationList.add(new Location("--All Locations--"));

        db.collection("Location")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                locationList.add(new Location(document.get("Location_name").toString()));
                            }

                            ArrayAdapter<Location> locationArrayAdapter = new ArrayAdapter<Location>(getActivity().getApplicationContext(),
                                    android.R.layout.simple_spinner_item, locationList);
                                 /**
                                   * Set the  Selected Location in the Spinner
                                   */
                            locationArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                            spinnerLocation.setAdapter(locationArrayAdapter);
                        } else {
                            Log.d("tag", "Error getting Locations: ", task.getException());
                            Toast.makeText(getActivity().getApplicationContext(), "Error getting Locations: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
    /**
     *This metthos id to get birds list with region
     * user see the search bird with name and image
     * if not then it display the error
     * @param searchText
     * @param region
     */

    private void getAllBirds(final String searchText, String region) {

        birdsList = new ArrayList<Bird>();
        Query query = null;

        if (region.equals("--All Locations--")) {
            query = db.collection("Birds");
        } else {
            query = db.collection("Birds").whereEqualTo("Location_name", region);
        }

        /*
        if (searchText.equals("")) {
            query = db.collection("Birds");
        } else {

            query = db.collection("Birds").whereGreaterThanOrEqualTo("name", searchText)
                    .whereLessThanOrEqualTo("name", searchText + "\uf8ff");
        }*/

        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            QuerySnapshot querySnapshot = task.getResult();

                            for (QueryDocumentSnapshot document : querySnapshot) {

                                final String birdName = document.getString("name");
                                if (birdName.contains(searchText)) {
                                    String birdID = document.getId();
                                    String birdImageURL = document.getString("birdimgUrl");
                                    birdsList.add(new Bird(birdID, birdName, birdImageURL));
                                }
                            }
                            if (birdsList.size() > 0) {
                                txt_no_search.setText("");
                            } else {
                                txt_no_search.setText("No birds found matching this Search Criteria.");
                            }
                            fillRecyclerView();
                        } else {
                            Log.d("tag", "Error getting birds: ", task.getException());
                            Toast.makeText(getActivity().getApplicationContext(), "Error getting birds: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    /**
     * this method shows view where user can click on search button
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_search) {
            btnSearchClick(v);
        }
    }
    /**
     *  this method display the list of all birds with location
     * @param v
     */

    private void btnSearchClick(View v) {

        Location location = (Location) spinnerLocation.getSelectedItem();

        String searchText = txtSearchBird.getText().toString();
        getAllBirds(searchText, location.getLocationName());
    }
    /**
     * The RecyclerView Method  will Display the List of Birds in Vertically with Names
     */

    private void fillRecyclerView() {

        RecyclerView recyclerView = getActivity().findViewById(R.id.recycler_view_search_birds);
        RecyclerView.LayoutManager recyclerLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        BirdsRecyclerAdapter recyclerAdapter = new BirdsRecyclerAdapter(birdsList, true);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.setOnItemClickListener(new BirdsRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {

//                Bundle bundle = new Bundle();
//                bundle.putParcelable("birds", birdsList.get(position));
                // navController.navigate(R.id.displayFragment, bundle);

                Intent intent = new Intent(getActivity(), BirdProfileActivity.class);
                intent.putExtra("birdID", birdsList.get(position).getBirdID());
                intent.putExtra("birdName", birdsList.get(position).getName());
                intent.putExtra("birdImageUrl", birdsList.get(position).getImageURL());
                getActivity().startActivity(intent);
            }
        });
    }
}
