try {
  if (rs.status().ok) {
    print('Replica set is already initialized.');
  }
} catch (e) {
  print('Initializing replica set...');
  rs.initiate({
    _id: 'rs0',
    members: [{ _id: 0, host: 'mongo:27017' }]
  });
  print('Replica set rs0 initialized successfully.');
}