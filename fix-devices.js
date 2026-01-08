// Fix devices missing type field
db = db.getSiblingDB('machinaear');
var result = db.devices.updateMany(
    { type: { $exists: false } },
    { $set: { type: "iot" } }
);
print("Updated " + result.modifiedCount + " devices");
print("Devices now:");
printjson(db.devices.find().toArray());
