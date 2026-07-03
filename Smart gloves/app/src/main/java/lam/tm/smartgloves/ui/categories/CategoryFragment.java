package lam.tm.smartgloves.ui.categories;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lam.tm.smartgloves.R;
import lam.tm.smartgloves.utils.FirebaseDatabaseHelper;

public class CategoryFragment extends Fragment {

	private RecyclerView recyclerView;
	private Button buttonAdd;
	private CategoriesAdapter adapter;
	private List<CategoryItem> categories;
	private DatabaseReference categoriesRef;
	private ValueEventListener categoriesListener;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_categories, container, false);
		recyclerView = root.findViewById(R.id.recycler_view_categories);
		buttonAdd = root.findViewById(R.id.button_add_category);

		// Setup back button
		View toolbarBack = root.findViewById(R.id.toolbar_back);
		if (toolbarBack != null) {
			android.widget.ImageButton buttonBack = toolbarBack.findViewById(R.id.button_back);
			android.widget.TextView textTitle = toolbarBack.findViewById(R.id.text_toolbar_title);
			if (buttonBack != null) {
				buttonBack.setOnClickListener(v -> {
					NavController navController = Navigation.findNavController(requireView());
					navController.navigateUp();
				});
			}
			if (textTitle != null) {
				textTitle.setText(getString(R.string.title_categories));
			}
		}

		categories = new ArrayList<>();
		adapter = new CategoriesAdapter(categories);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);

		categoriesRef = FirebaseDatabaseHelper.getCategoriesReference();
		categoriesListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (!isAdded() || getContext() == null || recyclerView == null || adapter == null || categories == null) {
					return;
				}
				
				categories.clear();
				for (DataSnapshot child : snapshot.getChildren()) {
					String key = child.getKey();
					String name = child.child("name").getValue(String.class);
					Integer nextId = child.child("nextId").getValue(Integer.class);
					if (name != null && !name.trim().isEmpty()) {
						CategoryItem item = new CategoryItem();
						item.key = key;
						item.name = name;
						item.nextId = (nextId != null ? nextId : 1);
						categories.add(item);
					}
				}
				Collections.sort(categories, (a, b) -> a.name.compareToIgnoreCase(b.name));
				
				if (adapter != null && recyclerView != null && isAdded()) {
					adapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
		};
		if (categoriesRef != null) {
			categoriesRef.addValueEventListener(categoriesListener);
		}

		buttonAdd.setOnClickListener(v -> showAddDialog());
		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (categoriesRef != null && categoriesListener != null) {
			categoriesRef.removeEventListener(categoriesListener);
		}
		recyclerView = null;
		buttonAdd = null;
		adapter = null;
		categories = null;
		categoriesRef = null;
		categoriesListener = null;
	}

	private void showAddDialog() {
		if (!isAdded() || getContext() == null || categoriesRef == null) {
			return;
		}
		
		final EditText input = new EditText(requireContext());
		input.setHint(getString(R.string.enter_category_name));
		new AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.add_category))
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!isAdded() || getContext() == null || categoriesRef == null) {
						return;
					}
					String name = input.getText().toString().trim();
					if (name.isEmpty()) {
						Toast.makeText(getContext(), getString(R.string.enter_category_name), Toast.LENGTH_SHORT).show();
						return;
					}
					String key = slugify(name);
					DatabaseReference ref = categoriesRef.child(key);
					java.util.Map<String, Object> values = new java.util.HashMap<>();
					values.put("name", name);
					values.put("nextId", 1);
					ref.updateChildren(values)
						.addOnSuccessListener(aVoid -> {
							// Success - listener will update UI
						})
						.addOnFailureListener(e -> {
							if (isAdded() && getContext() != null) {
								Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
							}
						});
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showEditDialog(CategoryItem item) {
		if (!isAdded() || getContext() == null || categoriesRef == null || item == null) {
			return;
		}
		
		final EditText input = new EditText(requireContext());
		input.setText(item.name);
		new AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.edit_category))
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!isAdded() || getContext() == null || categoriesRef == null) {
						return;
					}
					String name = input.getText().toString().trim();
					if (name.isEmpty()) return;
					categoriesRef.child(item.key).child("name").setValue(name)
						.addOnSuccessListener(aVoid -> {
							// Success - listener will update UI
						})
						.addOnFailureListener(e -> {
							if (isAdded() && getContext() != null) {
								Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
							}
						});
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void deleteCategory(CategoryItem item) {
		if (!isAdded() || getContext() == null || categoriesRef == null || item == null) {
			return;
		}
		
		new AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.delete_category))
				.setMessage(getString(R.string.confirm_delete_category, item.name))
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!isAdded() || getContext() == null || categoriesRef == null) {
						return;
					}
					categoriesRef.child(item.key).removeValue()
						.addOnSuccessListener(aVoid -> {
							// Success - listener will update UI
						})
						.addOnFailureListener(e -> {
							if (isAdded() && getContext() != null) {
								Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
							}
						});
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private String slugify(String input) {
		String s = input.trim().toLowerCase(Locale.ROOT);
		s = s.replaceAll("[^a-z0-9]+", "_");
		s = s.replaceAll("_+", "_");
		if (s.startsWith("_")) s = s.substring(1);
		if (s.endsWith("_")) s = s.substring(0, s.length()-1);
		if (s.isEmpty()) s = "cat" + System.currentTimeMillis();
		return s;
	}

	private class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {
		private final List<CategoryItem> items;
		CategoriesAdapter(List<CategoryItem> items) { this.items = items; }

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			CategoryItem item = items.get(position);
			holder.name.setText(item.name);
			holder.buttonEdit.setOnClickListener(v -> showEditDialog(item));
			holder.buttonDelete.setOnClickListener(v -> deleteCategory(item));
		}

		@Override
		public int getItemCount() { return items.size(); }

		class ViewHolder extends RecyclerView.ViewHolder {
			TextView name;
			Button buttonEdit;
			Button buttonDelete;
			ViewHolder(@NonNull View itemView) {
				super(itemView);
				name = itemView.findViewById(R.id.text_category_name);
				buttonEdit = itemView.findViewById(R.id.button_edit_category);
				buttonDelete = itemView.findViewById(R.id.button_delete_category);
			}
		}
	}
	
	private static class CategoryItem {
		String key;
		String name;
		int nextId;
	}
}

