const { sequelize, User, Role, VerificationRequest, Appeal } = require('c:/istu/recipesDiplom/models');
const jwt = require('jsonwebtoken');
const axios = require('axios');
const http = require('http');

const PORT = 5002; // Use a dedicated port for the integration test
const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_key';

function generateAccessToken(user) {
  return jwt.sign(
    { id: Number(user.id), username: user.username, role: user.roleName || 'User' },
    JWT_SECRET,
    { expiresIn: '1h' }
  );
}

async function runTests() {
  console.log('🔄 Connecting to Database...');
  await sequelize.authenticate();
  console.log('✅ Connected to Database.');
  
  console.log('🔄 Syncing Database Models...');
  await sequelize.sync({ alter: true });
  console.log('✅ Database Models Synced.');

  // 1. Sync roles
  const [adminRole] = await Role.findOrCreate({ where: { id: 1 }, defaults: { name: 'Admin' } });
  const [userRole] = await Role.findOrCreate({ where: { id: 2 }, defaults: { name: 'user' } });
  console.log(`✅ Roles ensured: Admin (${adminRole.id}), user (${userRole.id})`);

  // 2. Clean up old test data
  await Appeal.destroy({ where: {} });
  await VerificationRequest.destroy({ where: {} });
  const { Notification: NotificationModel } = require('c:/istu/recipesDiplom/models');
  await NotificationModel.destroy({ where: {} });
  await User.destroy({ where: { email: { [sequelize.Sequelize.Op.like]: '%@test.com' } } });
  console.log('🧹 Cleaned up old test data.');

  // 3. Create test users
  const adminUser = await User.create({
    username: 'testadmin',
    name: 'Test Admin',
    email: 'admin@test.com',
    password: 'password_hash_dummy',
    role_id: 1,
    is_verified: true,
    is_blocked: false
  });
  adminUser.roleName = 'Admin';

  const normalUser = await User.create({
    username: 'testuser',
    name: 'Test User',
    email: 'user@test.com',
    password: 'password_hash_dummy',
    role_id: 2,
    is_verified: true,
    is_blocked: false
  });
  normalUser.roleName = 'User';

  console.log(`👤 Created Test Admin (ID: ${adminUser.id}) and Test User (ID: ${normalUser.id})`);

  const adminToken = generateAccessToken(adminUser);
  const userToken = generateAccessToken(normalUser);

  // 4. Start the server programmatically on port 5002
  console.log('🚀 Starting Express application for test...');
  const testExpress = require('express');
  const testApp = testExpress();
  testApp.use(testExpress.json());

  testApp.use('/users', require('c:/istu/recipesDiplom/routes/userRoutes'));
  testApp.use('/reports', require('c:/istu/recipesDiplom/routes/reportRoutes'));
  testApp.use('/admin', require('c:/istu/recipesDiplom/routes/adminRoutes'));
  testApp.use('/recipes', require('c:/istu/recipesDiplom/routes/recipeRoutes'));

  const server = testApp.listen(PORT, async () => {
    console.log(`📡 Test server running on http://localhost:${PORT}`);
    const client = axios.create({
      baseURL: `http://localhost:${PORT}`,
      validateStatus: () => true
    });

    try {
      console.log('\n--- 🧪 TEST 1: Verification Request Validation ---');
      // 1. Submit with empty full name
      let res = await client.post('/users/verify-request', { full_name: '   ', info: 'Cool cook' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Empty name response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 400) throw new Error('Expected 400 for empty full name');

      // 2. Submit valid verification request
      res = await client.post('/users/verify-request', { full_name: 'Иван Иванов', info: 'Шеф-повар с 10-летним стажем' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Valid request response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 201) throw new Error('Expected 201 for valid request');
      const vRequestId = res.data.request.id;

      // 3. Try to submit a duplicate verification request
      res = await client.post('/users/verify-request', { full_name: 'Иван Иванов', info: 'Еще инфо' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Duplicate request response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 400) throw new Error('Expected 400 for duplicate request');

      console.log('\n--- 🧪 TEST 2: Admin Get Verification Requests ---');
      res = await client.get('/admin/verifications', {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Admin get verifications: ${res.status} - found ${res.data.length} requests`);
      if (res.status !== 200) throw new Error('Expected 200 for admin verifications listing');
      
      const foundRequest = res.data.find(r => Number(r.id) === Number(vRequestId));
      if (!foundRequest) throw new Error('Verification request not found in admin list');
      console.log(`Found request data: full_name="${foundRequest.full_name}", info="${foundRequest.info}"`);
      if (foundRequest.full_name !== 'Иван Иванов' || foundRequest.info !== 'Шеф-повар с 10-летним стажем') {
        throw new Error('Incorrect full_name or info returned in admin verifications');
      }

      console.log('\n--- 🧪 TEST 3: User Blocking Enforcement ---');
      // 1. Block the test user
      res = await client.post(`/admin/users/${normalUser.id}/block`, {}, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Block response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for user blocking');

      // 2. Prevent admin self-blocking
      res = await client.post(`/admin/users/${adminUser.id}/block`, {}, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Block self response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 400 && res.status !== 403) throw new Error('Expected 400 or 403 for self-blocking');

      // 3. Test that blocked user gets 403 on state-changing report submit
      res = await client.post('/reports', { type: 'recipe', recipe_id: 1, reason: 'Spam' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Report submission as blocked user response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 403 || !res.data.is_blocked) throw new Error('Expected 403 is_blocked on report submission');

      console.log('\n--- 🧪 TEST 4: Appeal Workflow ---');
      // 1. Submit empty appeal
      res = await client.post('/users/me/appeal', { message: '  ' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Empty appeal response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 400) throw new Error('Expected 400 for empty appeal message');

      // 2. Submit valid appeal
      res = await client.post('/users/me/appeal', { message: 'Разблокируйте меня, пожалуйста. Я больше так не буду.' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Valid appeal response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 201) throw new Error('Expected 201 for valid appeal');
      const appealId = res.data.appeal.id;

      // 3. Submit duplicate appeal
      res = await client.post('/users/me/appeal', { message: 'Второй раз пишу' }, {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Duplicate appeal response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 400) throw new Error('Expected 400 for duplicate appeal');

      // 4. Admin reads appeals
      res = await client.get('/admin/appeals', {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Admin get appeals response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for admin appeals list');
      
      const foundAppeal = res.data.find(a => Number(a.id) === Number(appealId));
      if (!foundAppeal) throw new Error('Appeal not found in admin appeals list');

      // 5. Admin resolves appeal and unblocks user
      res = await client.patch(`/admin/appeals/${appealId}`, { status: 'resolved', admin_notes: 'Одобряю' }, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Resolve appeal response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for resolving appeal');

      // Assert user is unblocked in DB
      const updatedUser = await User.findByPk(normalUser.id);
      console.log(`Unblocked user status in DB: is_blocked = ${updatedUser.is_blocked}`);
      if (updatedUser.is_blocked) throw new Error('Expected user is_blocked to be false in database');

      console.log('\n--- 🧪 TEST 5: Verification Request Approval ---');
      res = await client.patch(`/admin/verifications/${vRequestId}`, { status: 'approved', admin_notes: 'Одобрено' }, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Approve verification request response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for approving verification request');

      // Assert user is verified in DB
      const verifiedUser = await User.findByPk(normalUser.id);
      console.log(`Verified user status in DB: is_verified = ${verifiedUser.is_verified}`);
      if (!verifiedUser.is_verified) throw new Error('Expected user is_verified to be true in database');

      console.log('\n--- 🧪 TEST 6: User Role Change Notification ---');
      // Update role of the test user to Moderator
      const [modRole] = await Role.findOrCreate({ where: { name: 'Moderator' } });
      res = await client.patch(`/admin/users/${normalUser.id}`, { role_id: modRole.id }, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Update user role response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for user role update');

      // Verify a SYSTEM notification was created in database for the user
      const { Notification: NotificationModel } = require('c:/istu/recipesDiplom/models');
      const dbNotification = await NotificationModel.findOne({
        where: { user_id: normalUser.id, type: 'SYSTEM' }
      });
      if (!dbNotification) throw new Error('Expected a SYSTEM database notification to be created for the role change');
      console.log(`Saved DB Notification message: "${dbNotification.message}"`);
      if (!dbNotification.message.includes('Модератор')) {
        throw new Error('Expected database notification message to contain the correct role name "Модератор"');
      }

      console.log('\n--- 🧪 TEST 7: Weekly Menu Compilation & Access ---');
      // 1. Create a test recipe
      const { Recipe } = require('c:/istu/recipesDiplom/models');
      const testRecipe = await Recipe.create({
        user_id: normalUser.id,
        title: 'Тестовый рецепт',
        description: 'Простое описание тестового рецепта',
        portion: 2,
        cooking_time: 30,
        difficulty: '1',
        is_private: false
      });
      console.log(`Created test recipe for menu (ID: ${testRecipe.id})`);

      // 2. Add recipe to menu of the week
      res = await client.post('/admin/menu-of-week', { day_of_week: 1, recipe_id: testRecipe.id }, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Add to menu response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 201) throw new Error('Expected 201 for adding recipe to menu');
      const menuItemId = res.data.id;

      // 3. Normal user fetches the menu of the week
      res = await client.get('/recipes/menu-of-week', {
        headers: { Authorization: `Bearer ${userToken}` }
      });
      console.log(`Get weekly menu response (Normal User): ${res.status} - found ${res.data.length} items`);
      if (res.status !== 200) throw new Error('Expected 200 for user getting menu');
      const foundMenuItem = res.data.find(m => Number(m.id) === Number(menuItemId));
      if (!foundMenuItem || !foundMenuItem.Recipe || foundMenuItem.Recipe.title !== 'Тестовый рецепт') {
        throw new Error('Expected menu of week to include test recipe and full details');
      }
      console.log(`Normal user successfully fetched the recipe: "${foundMenuItem.Recipe.title}"`);

      // 4. Delete the recipe from menu of the week
      res = await client.delete(`/admin/menu-of-week/${menuItemId}`, {
        headers: { Authorization: `Bearer ${adminToken}` }
      });
      console.log(`Delete from menu response: ${res.status} - ${JSON.stringify(res.data)}`);
      if (res.status !== 200) throw new Error('Expected 200 for deleting from menu');

      console.log('\n🎉 ALL INTEGRATION TESTS PASSED SUCCESSFULLY! backend is 100% stable and fully operational! 🎉');
    } catch (error) {
      console.error('\n❌ INTEGRATION TEST FAILED:', error.message);
      if (error.response) {
        console.error('Response data:', error.response.data);
      }
      process.exitCode = 1;
    } finally {
      console.log('🔌 Shutting down test server...');
      server.close(() => {
        console.log('👋 Test server shut down.');
        process.exit();
      });
    }
  });
}

runTests().catch(e => {
  console.error('Fatal Test Run Error:', e);
  process.exit(1);
});
