# Getting started


Install [reverie/lein-template "1.2.0"] as a plugin in your profiles.clj under .lein. 

1. Run lein new reverie <name-of-project> in the directory where you want the project installed.
2. Answer the questions that pops up.
3. Install postgresql if you don't have it already.
4. Setup user credentials and the database as per what you filled in step 2.
5. Navigate to the newly created directory <name-of-project>
6. Type 'lein run :command :init' and press enter. Answer the questions. Once finished the database should be populated with the bare essentials for a functioning site.
7. Fire up your editor and go into REPL mode. Navigate to dev.clj and follow instructions.
8. Navigate to 127.0.0.1:3000 for the website once it's up and running. Admin is available under 127.0.0.1:3000/admin
