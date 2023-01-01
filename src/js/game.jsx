//not sure if needed
import React from 'react';
import PropTypes from 'prop-types';

// class Board extends React.Component{

// }

class Game extends React.Component{
    render() {
        return (
            <div className = "game">
                <p>React is rendering</p>
            </div> 
        );
    }
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(<Game />);