const API_URL = 'http://localhost:5000';

const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MzYsInVzZXJuYW1lIjoidGVzdHVzZXJfMTc3ODYzMjY1Nzk0NiIsInJvbGUiOiJVc2VyIiwiaWF0IjoxNzc4NjMyNjU4LCJleHAiOjE3Nzg3MTkwNTh9.ulhoIb5gBzybpmtjW3klJ2HxrHL36_bNwUp2Kibfh5w';

async function runTests() {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  let recipeId;

  console.log('\n--- Test 1: Create Recipe ---');
  try {
    const res = await fetch(`${API_URL}/recipes`, {
      method: 'POST', headers,
      body: JSON.stringify({
        title: 'Test Recipe', description: 'Desc', difficulty: '2', portion: 4, cooking_time: 30,
        ingredients: [{ name: 'Tomato', quantity: '2', unit: 'шт' }],
        steps: [{ description: 'Chop' }]
      })
    });
    const data = await res.json();
    recipeId = data.id;
    console.log('Created ID:', recipeId, 'Ings:', data.Ingredients?.length, 'Steps:', data.Steps?.length);
  } catch (e) { console.error('ERROR:', e.message); }

  console.log('\n--- Test 2: Update Recipe with empty arrays ---');
  if (recipeId) {
    try {
      const res = await fetch(`${API_URL}/recipes/${recipeId}`, {
        method: 'PUT', headers,
        body: JSON.stringify({
          title: 'Updated', description: 'Desc', difficulty: '2', portion: 4, cooking_time: 30,
          ingredients: [], steps: []
        })
      });
      const data = await res.json();
      console.log('Update response Ings:', data.Ingredients?.length, 'Steps:', data.Steps?.length);
      
      const res2 = await fetch(`${API_URL}/recipes/${recipeId}`, { headers });
      const data2 = await res2.json();
      console.log('GET response Ings:', data2.Ingredients?.length, 'Steps:', data2.Steps?.length);
    } catch (e) { console.error('ERROR:', e.message); }
  }
}

runTests();
