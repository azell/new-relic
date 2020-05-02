I've linked a copy of a template project that you may wish to use to bootstrap the build process, but there's absolutely no requirement to use it if you don't wish to.

https://drive.google.com/file/d/1tI5ZQeSpzYRlqjCEA_nCGVGi60chEnnr/view?usp=sharing

Code Challenge Instructions:

Write a server (“Application”) in Java that opens a socket and restricts input to at most 5 concurrent clients. Clients will connect to the Application and write any number of 9 digit numbers, and then close the connection. The Application must write a de-duplicated list of these numbers to a log file in no particular order.

Primary Considerations

The Application should work correctly as defined below in Requirements.
The overall structure of the Application should be simple.
The code of the Application should be descriptive and easy to read, and the build method and runtime parameters must be well-described and work.
The design should be resilient with regard to data loss.
The Application should be optimized for maximum throughput, weighed along with the other Primary Considerations and the Requirements below.

Requirements

The Application must accept input from at most 5 concurrent clients on TCP/IP port 4000.
Input lines presented to the Application via its socket must either be composed of exactly nine decimal digits (e.g.: 314159265 or 007007009) immediately followed by a server-native newline sequence; or a termination sequence as detailed in #9, below.
Numbers presented to the Application must include leading zeros as necessary to ensure they are each 9 decimal digits.
The log file, to be named "numbers.log”, must be created anew and/or cleared when the Application starts.
Only numbers may be written to the log file. Each number must be followed by a server-native newline sequence.
No duplicate numbers may be written to the log file.
Any data that does not conform to a valid line of input should be discarded and the client connection terminated immediately and without comment.

Every 10 seconds, the Application must print a report to standard output:
The difference since the last report of the count of new unique numbers that have been received.
The difference since the last report of the count of new duplicate numbers that have been received.
The total number of unique numbers received for this run of the Application.

Example text for #8: Received 50 unique numbers, 2 duplicates. Unique total: 567231

If any connected client writes a single line with only the word "terminate" followed by a server-native newline sequence, the Application must disconnect all clients and perform a clean shutdown as quickly as possible.

Clearly state all of the assumptions you made in completing the Application.
Tests are provided to exercise all of the primary considerations and requirements.

Notes

You may use common libraries in your project such as Apache Commons and Google Guava, particularly if their use helps improve Application simplicity and readability. However the use of large frameworks, such as Akka, is prohibited.
Your Application may not for any part of its operation use or require the use of external systems, for example Apache Kafka or Redis.
At your discretion, leading zeroes present in the input may be stripped—or not used—when writing output to the log or console.
Robust implementations of the Application typically handle more than 2M numbers per 10-second reporting period on a modern MacBook Pro laptop (e.g.: 16 GiB of RAM and a 2.5 GHz Intel i7 processor
