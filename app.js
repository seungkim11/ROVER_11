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
//a user will send their xy sciencecoordinates and get back the science
//in order from closest and on using distance heursitic
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



app.get("/allScience", function(req, res) {
	res.json(science); //for now
//this currently returns the hardcoded value.
});

 app.get('/',function(req,res){
 	res.writeHead(200,{"Content-Type":"text"});
 	res.write("Ok..., type a / and a tool name your bot has to obtain available resources. \n For example: 192.168.1.1:3000/harvester ");
 	res.end();

 });

app.get('/:tool', function(req,res){
	scienceDB.find({"tool": req.params.tool,"stillExists": true}).toArray(function(err,docs){
		if(err) throw err;
		(res.send(docs));
	});
});

	 ////
	////POST
   ////
app.post("/allScience", function(req, res) {
	console.log(req.body);
	data_={};
	science.push(data_);
    res.json(science);
});



app.listen(3000);

console.log("Express app running on port 3000");

module.exports = app;