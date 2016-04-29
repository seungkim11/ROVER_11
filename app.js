//By Peter Bryan Rodriguez
//Communication Server For Green Corp
var express = require("express");
var bodyParser = require("body-parser");
var app = express();


//a user will send their xy coordinates and get back the science
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

//app.use(express.static("./public"));

//app.use(cors());

app.get("/allScience", function(req, res) {
	res.json(science);
});

app.post("/allScience", function(req, res) {
	//I need to do checking here to make sure it is all in the right format
	console.log(req.body);
	//extract variables

	//put data in an JSONobject data_;
	data_={};
	science.push(data_);
    //science.push(req.body); 
    res.json(science);
});

app.listen(3000);

console.log("Express app running on port 3000");

module.exports = app;