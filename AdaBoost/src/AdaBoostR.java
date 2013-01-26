/** 
 * AdaBoostR.java
 * An implementation of AdaBoost for regression. Based on the algorithm in 
 * Zemel, R. S., & Pitassi, T. (2001). A gradient-based boosting algorithm for 
 * regression problems. Advances in neural information processing systems, 696-702.
 * 
 * @author Michelle Shu
 * January 25, 2012
 */

/* Plan:
 * 
 * 1. Input
 * 	Use training set examples (x1, y1)... (xn, yn)
 * 	Train many weak learners who produce hypothesis f(x) that approximates y
 * 		and whose performance can be evaluated by cost function J
 * 
 * 2. Set initial weights of all training inputs: p(xi) = 1/n
 * 
 * 3. Iterate:
 * 		Call WeakLearner - minimize cost by reweighting distribution p
 * 		(Test by acceptance criteria whether WeakLearner is good enough)
 * 		Use gradient descent to set 0 <= c <= 1, the combination coefficients
 * 			of recruited learners
 * 		Update the training distribution
 * 
 */
