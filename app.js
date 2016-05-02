//By PBR
//
//Communication Server For Green Corp
var express = require("express");
var bodyParser = require("body-parser");
var mongo= require("mongodb");
var http=require('http');
var app = express();

var mongojs = require('mongojs')
var db = mongojs('test', ['test']);
var scienceDB = db.collection('test');


//This is an example reminder of how the JSON objects look:

var science = [
    //Sample Data
    {
        x:12,
        y:14,
        terrain: "sand",
        tool: "harvester",
        stillExists: true
    }
    
];

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

app.use(function(req, res, next) {
	console.log(`${req.method} request for '${req.url}' - ${JSON.stringify(req.body)}`);
	next();
});

	//Instructions incase someone hits the :3000 server without api.
 app.get('/',function(req,res){
 	res.writeHead(200,{"Content-Type":"text"});
 	res.write("Ok..., type a / and a tool name your bot has to obtain available resources. \n For example: 192.168.1.222:3000/harvester/walker \n If you have two tools, try this format: 192.168.1.222:3000/harvester/thread/drill \n if you're a scout, send POST requests to http:://192.168.1.222:3000/scout");
 	res.end();

 });



//If the user only has one tool, this is the API call they will use.

app.get('/:tool/:vehicle', function(req,res){
	//if being requested by a wheel

	switch(req.params.vehicle){

		case "wheel":
			console.log("User requested results of wheels");
			 	scienceDB.find({"tool": req.params.tool,"terrain": "normal", "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;

		case "walker":
			console.log("User requested results of walker");
			scienceDB.find({"tool": req.params.tool,"terrain": {$in: ['rock', 'normal']}, "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;

		case "tread":
		console.log("User requested results of tread");
		scienceDB.find({"tool": req.params.tool,"terrain": {$in: ['sand', 'normal']}, "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;
	}

 }); // 1 tool api end.



//If the user has two tools, this is what they will use.
app.get('/:tool/:vehicle/:tool2', function(req,res){
	//if being requested by a wheel

	switch(req.params.vehicle){

		case "wheel":
			console.log("User requested results of wheels - 2 tools");
			 	scienceDB.find({"tool": {$in:[req.params.tool, req.params.tool2]},"terrain": "normal", "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;

		case "walker":
			console.log("User requested results of walker- 2 tools");
			scienceDB.find({"tool": {$in:[req.params.tool, req.params.tool2]},"terrain": {$in: ['rock', 'normal']}, "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;

		case "tread":
		console.log("User requested results of tread- 2 tools");
		scienceDB.find({"tool": {$in:[req.params.tool, req.params.tool2]},"terrain": {$in: ['sand', 'normal']}, "stillExists": true}).toArray(function(err,docs){
	 		if(err) throw err;
	 		(res.send(docs));
	 		});
		break;
	}

 }); // 2 tool api end.





// 		/////For testing purposes i'll leave this older call
// app.get('/:tool', function(req,res){
// 	scienceDB.find({"tool": req.params.tool,"stillExists": true}).toArray(function(err,docs){
// 		if(err) throw err;
// 		(res.send(docs));
// 	});
// });



//Global Map - This Will Also Show Gathered Science, for future debugging.
app.get('/globalMap', function(req,res){
	scienceDB.find().toArray(function(err,docs){
		if(err) throw err;
		(res.send(docs));
	});
});


	 ////
	////POST method Has not Been Implemented Yet.
   ////
app.post("/scout", function(req, res) {
	
	var data_={};
	data_.x=req.body.x;
	data_.y=req.body.y;
	data_.terrain=req.body.terrain;
	data_.tool=req.body.tool;
	data_.stillExists=req.body.stillExists;
	console.log("data_ looks like this:");
	console.log(data_);
	
});



//// TODO: Implement a POST method for harvester/drillers that changes the value
/// of a harvested science [by x, and y] and changes it's stillExists condition to 'false'
//


//I'm still debating whether to have the server send back the coordinates in order or let the client do that.
//because only the client will know how far it really is depending on their A* search based on their position 
//and surronding


app.listen(3000);

console.log("Express app running on port 3000");

module.exports = app;